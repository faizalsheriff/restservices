package com.hp.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.example.controllers.PersonController;
import com.hp.example.entities.Person;
import com.hp.example.services.PersonService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PersonControllerTest {
    @Autowired
    private PersonController controller;                  // the controller under test
    @Autowired
    private PersonService service;                        // the mock service
    private MockMvc mockMvc;                              // the mock Spring MVC fluent bean

    @PostConstruct
    public void setup() {
        // create the mock Spring MVC fluent bean
        mockMvc = MockMvcBuilders.standaloneSetup(controller)  // create it only for our controller
                // add the message converters
                .setMessageConverters(new MappingJackson2HttpMessageConverter(), new StringHttpMessageConverter())
                // and build it
                .build();
    }

    @Test
    public void json() throws Exception {
        // setup expectations on the service mock
        when(service.readAllPersons()).thenReturn(Arrays.asList(new Person("Moshe", "Cohen"), new Person("Yakov", "Levi")));

        // perform the request
        mockMvc.perform(get("/persons").accept(MediaType.APPLICATION_JSON))         // get /persons, accept json
                .andExpect(status().isOk())                                         // expect the status to be ok
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))       // expect json result
                .andExpect(jsonPath("$.", filter(where("firstName").is("Moshe"))).exists())  // expect Moshe in the list
                .andExpect(jsonPath("$..[?(@.firstName == 'Moshe')]").exists());    // same same (but different syntax)
    }

    @Test
    public void expectError() throws Exception {
        //noinspection unchecked
        when(service.readAllPersons()).thenThrow(IOException.class);

        mockMvc.perform(get("/persons").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("\"f^@k\""));
    }

    @Test
    public void createPerson() throws Exception {
        when(service.createPerson(anyString(), anyString())).thenReturn(42l);

        ObjectMapper mapper = new ObjectMapper();
        String charlie = mapper.writeValueAsString(new Person("Charlie", "Brown"));

        mockMvc.perform(post("/persons").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(charlie))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(is("42")));
    }

    @Test
    public void findPerson() throws Exception {
        when(service.readPerson(77l)).thenReturn(createPersonWithId(77l, "Jesus", "Ben Joseph"));

        mockMvc.perform(get("/persons/{id}", 77l).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(is(77)))
                .andExpect(jsonPath("$.firstName").value(is("Jesus")));

        mockMvc.perform(get("/persons/{id}", 1l).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deletePerson() throws Exception {
        mockMvc.perform(delete("/persons/{id}", 412l).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service).deletePerson(412l);
    }

    @Test
    public void createInvalidPerson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String invalid = mapper.writeValueAsString(new Person("", ""));

        mockMvc.perform(post("/persons").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPerson(anyString(), anyString());
    }

    /**
     * this is required because the Person does not expose its id property for writing on purpose - only hibernate
     * needs write access to the id
     */
    private Person createPersonWithId(long id, String firstName, String lastName) throws NoSuchFieldException, IllegalAccessException {
        Person p = new Person(firstName, lastName);

        Field f = Person.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(p, id);

        return p;
    }

    @Configuration
    @ComponentScan(basePackages = "com.hp.example.controllers")
    public static class PersonControllerTestConfiguration {
        @Bean
        public PersonService personService() {
            return mock(PersonService.class);
        }
    }
}
