package com.hp.example;

import com.hp.example.entities.Person;
import com.hp.example.services.PersonService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration                // make this a web application context test
public class PersonViewTest {
    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private PersonService service;
    private MockMvc mockMvc;

    @PostConstruct
    private void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void csv() throws Exception {
        when(service.readAllPersons()).thenReturn(Arrays.asList(new Person("Moshe", "Cohen"), new Person("Yakov", "Levi")));

        getPersonsForMediaType("text/csv");
    }

    @Test
    public void excel() throws Exception {
        when(service.readAllPersons()).thenReturn(Arrays.asList(new Person("Moshe", "Cohen"), new Person("Yakov", "Levi")));

        getPersonsForMediaType("application/vnd.ms-excel");
    }

    private MvcResult getPersonsForMediaType(String mediaType) throws Exception {
        return mockMvc.perform(get("/persons").accept(MediaType.parseMediaType(mediaType)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(mediaType)).andReturn();
    }

    @Configuration
    @ImportResource("WEB-INF/rest-servlet.xml")      // import the bean definitions from the XML
    public static class PersonViewTestConfiguration {
        @Bean
        public PersonService personService() {
            return mock(PersonService.class);
        }
    }
}
