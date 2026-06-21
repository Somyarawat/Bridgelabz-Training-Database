package com.addressbook.util;

import com.addressbook.model.Contact;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public static List<Contact> parseContacts(String csvFilePath)
            throws IOException {

        List<Contact> contacts = new ArrayList<>();

        BufferedReader br =
                new BufferedReader(
                        new FileReader(csvFilePath));

        String line;

        // Skip Header Row
        br.readLine();

        while ((line = br.readLine()) != null) {

            String[] data = line.split(",");

            if (data.length >= 5) {

                Contact contact = new Contact(
                        data[0].trim(), // First Name
                        data[1].trim(), // Last Name
                        data[2].trim(), // Phone
                        data[3].trim(), // Email
                        data[4].trim()  // Address
                );

                contacts.add(contact);
            }
        }

        br.close();

        return contacts;
    }
}