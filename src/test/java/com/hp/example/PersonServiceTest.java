package com.hp.example;

import com.hp.example.entities.Person;
import com.hp.example.repositories.PersonRepository;
import com.hp.example.services.PersonService;
import com.hp.example.services.PersonServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PersonServiceTest {
    @Autowired
    private PersonService personService;
    @Autowired
    private PersonRepository personRepository;

    @Test
    public void readAllPersons() {
        // setup expectations
        when(personRepository.findAll()).thenReturn(Arrays.asList(new Person("George", "Washington")));

        List<Person> persons = personService.readAllPersons();
        assertThat("the service returned a null list", persons, is(notNullValue()));
        assertThat("the service didn't return any persons", persons.size(), is(greaterThan(0)));

        Person mosheCohen = new Person();
        mosheCohen.setFirstName("George");
        mosheCohen.setLastName("Washington");

        assertThat("Moshe Cohen is not in the list", mosheCohen, isIn(persons));
    }

    @Test
    public void createPerson() {
        final List<Person> personsDb = new ArrayList<>(2);
        personsDb.add(new Person("George", "Washington"));

        when(personRepository.findAll()).thenReturn(personsDb);

        List<Person> persons = personService.readAllPersons();
        assertThat("the service returned a null list", persons, is(notNullValue()));
        assertThat("the service didn't return any persons", persons.size(), is(greaterThan(0)));

        int beforeSize = persons.size();

        when(personRepository.save((Person) anyObject())).thenAnswer(new Answer<Person>() {
            @Override
            public Person answer(InvocationOnMock invocationOnMock) throws Throwable {
                // fake a database - add the created person to a list
                Person p = (Person) invocationOnMock.getArguments()[0];
                personsDb.add(p);
                Field f = Person.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(p, (long) personsDb.size());
                return p;
            }
        });

        long id = personService.createPerson("Elizabeth", "Windsor");
        assertThat("zero ID is unacceptable", id, is(greaterThan(0l)));
        assertThat("there aren't enough persons", personService.readAllPersons().size(), is(greaterThan(beforeSize)));
    }

    @Test
    public void readPerson() {
        when(personRepository.findOne(anyLong())).thenReturn(new Person("George", "Washington"));
        assertThat("couldn't read person 1", personService.readPerson(1l), is(notNullValue()));
    }

    @Configuration
    public static class PersonServiceTestConfiguration {
        @Bean
        public PersonService personService() {
            return new PersonServiceImpl();
        }

        @Bean
        public PersonRepository personRepository() {
            return mock(PersonRepository.class);
        }
    }
}
