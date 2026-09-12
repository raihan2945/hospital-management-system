package com.example.hms;

import com.example.hms.config.DemoDataInitializer;
import com.example.hms.config.RegistrationCodeInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalManagementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/dashboard"})
    void homeRendersWithSharedLayout(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Hospital Management System")))
                .andExpect(content().string(containsString("/actuator/health")))
                .andExpect(content().string(containsString("View billing")));
    }

    @Test
    void healthChecksTheDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/css/app.css", "/webjars/bootstrap/5.3.8/css/bootstrap.min.css",
            "/webjars/bootstrap/5.3.8/js/bootstrap.bundle.min.js"})
    void layoutAssetsAreServedLocally(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void configurationEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    void demoDataIsNotSeededUnlessItIsRequested() {
        assertThat(context.getBeansOfType(DemoDataInitializer.class)).isEmpty();
        assertThat(context.getBeansOfType(RegistrationCodeInitializer.class)).hasSize(1);
    }
}
