package com.addressbook.dao;

import com.addressbook.model.Contact;
import java.util.List;

public interface ContactDAO {
    void addContact(Contact contact) throws Exception;
    List<Contact> getAllContacts() throws Exception;
    List<Contact> searchContacts(String keyword) throws Exception;
    void updateContact(Contact contact) throws Exception;
    void deleteContact(int id) throws Exception;
    void importContactsBatch(List<Contact> contacts) throws Exception;
}