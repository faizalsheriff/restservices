package com.hp.example.services;

import com.hp.example.entities.Person;

import java.util.List;

public interface PersonService {
    List<Person> readAllPersons();

    long createPerson(String firstName, String lastName);

    Person readPerson(long id);

    void deletePerson(long id);
}
