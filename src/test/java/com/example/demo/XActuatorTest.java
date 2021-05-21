package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith(SpringExtension.class) // required for Spring + Junit 5 DI Integration
@SpringBootTest
@AutoConfigureMockMvc
// Metrics are disabled since 2.4.0 for spring integration tests by default to increase load up in tests
@AutoConfigureMetrics
// used to overwrite the custom port of management.server because MockMvc doesn't know about it.
@TestPropertySource(properties = {"management.server.port="})
public class XActuatorTest {

  @Autowired
  private MockMvc mockMvc;

  @Value("${management.metrics.tags.application}")
  private String applicationValue;

  @Value("${management.metrics.tags.env}")
  private String envValue;

  @Test
  protected void testHealthEndpoint() throws Exception {
    mockMvc
      .perform(get("/actuator/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(equalTo(Status.UP.toString())));
  }

  @Test
  protected void testPrometheusEndpoint() throws Exception {
    mockMvc
      .perform(get("/actuator/prometheus"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("system_cpu_count")));
  }

  @Test
  protected void testPrometheusJvmEndpoint() throws Exception {
    mockMvc
      .perform(get("/actuator/prometheus"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("jvm_memory_used_bytes")));
  }

  @Test
  protected void testActuatorJvmEndpoint() throws Exception {
    mockMvc
      .perform(get("/actuator/metrics/jvm.memory.used"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(equalTo("jvm.memory.used")));
  }

  @Test
  protected void testPrometheusCustomTags() throws Exception {
    MvcResult result = mockMvc
      .perform(get("/actuator/prometheus"))
      .andExpect(status().isOk())
      .andReturn();

    String body = result.getResponse().getContentAsString();

    assertThat(body)
      .contains(
        "application=\"" + applicationValue + "\"",
        "env=\"" + envValue + "\""
      );
  }

  @Test
  protected void testDisabledMetricsShouldNotBePresent() throws Exception {
    MvcResult result = mockMvc
      .perform(get("/actuator/prometheus"))
      .andExpect(status().isOk())
      .andReturn();

    assertThat(result.getResponse().getContentAsString())
      .doesNotContain("http_server_requests_seconds_count");
  }
}
