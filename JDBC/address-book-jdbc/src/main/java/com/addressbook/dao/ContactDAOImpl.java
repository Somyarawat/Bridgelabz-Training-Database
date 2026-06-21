package com.addressbook.dao;

import com.addressbook.config.DatabaseConfig;
import com.addressbook.model.Contact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactDAOImpl implements ContactDAO {

    @Override
    public void addContact(Contact contact) throws SQLException {
        // TODO: Insert a contact using PreparedStatement. Wrap with try-with-resources.
        String sql = """
                INSERT INTO contacts
                (first_name, last_name, phone, email, address)
                VALUES(?,?,?,?,?)
                """;

        try(
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
                ) {
            ps.setString(1, contact.getFirstName());
            ps.setString(2, contact.getLastName());
            ps.setString(3, contact.getPhone());
            ps.setString(4, contact.getEmail());
            ps.setString(5, contact.getAddress());

            ps.executeUpdate();
            System.out.println("Contact Added Successfully");
        }
    }

    @Override
    public List<Contact> getAllContacts() throws SQLException {
        // TODO: Retrieve all contacts sorted by last_name, first_name. Map rows to list.
        List<Contact> contacts = new ArrayList<>();

        String sql = """
                SELECT * FROM contacts
                ORDER BY first_name, last_name
                """;

        try(
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                ) {
            while(rs.next()) {
                Contact contact = new Contact();

                contact.setID(rs.getInt("id"));
                contact.setFirstName(rs.getString("first_name"));
                contact.setLastName(rs.getString("last_name"));
                contact.setPhone(rs.getString("phone"));
                contact.setEmail(rs.getString("email"));
                contact.setAddress(rs.getString("address"));

                contacts.add(contact);
            }
        }
        return contacts;
    }

    @Override
    public List<Contact> searchContacts(String keyword) throws SQLException {
        // TODO: Wildcard ILIKE search on first_name, last_name, or email. Use parameterized inputs.
        List<Contact> contacts = new ArrayList<>();

        String sql = """
                SELECT * FROM contacts
                WHERE first_name ILIKE ?
                OR last_name ILIKE ?
                OR email ILIKE ?
                """;

        try (
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ) {
            String search = "%" + keyword + "%";
            ps.setString(1, search);
            ps.setString(2, search);
            ps.setString(3, search);

            ResultSet rs = ps.executeQuery();

            while(rs.next()) {
                Contact contact = new Contact();
                contact.setID(rs.getInt("id"));
                contact.setFirstName(rs.getString("first_name"));
                contact.setLastName(rs.getString("last_name"));
                contact.setPhone(rs.getString("phone"));
                contact.setEmail(rs.getString("email"));
                contact.setAddress(rs.getString("address"));

                contacts.add(contact);
            }
        }
        return contacts;
    }

    @Override
    public void updateContact(Contact contact) throws SQLException {
        // TODO: Update fields (first_name, last_name, phone, email, address) of contact by ID.
        String sql = """
                UPDATE contacts
                SET first_name=?,
                last_name=?,
                phone=?,
                email=?,
                address=?
                WHERE id=?
                """;

        try(Connection conn = DatabaseConfig.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, contact.getFirstName());
            ps.setString(2, contact.getLastName());
            ps.setString(3, contact.getPhone());
            ps.setString(4, contact.getEmail());
            ps.setString(5, contact.getAddress());
            ps.setInt(6, contact.getID());

            int rows = ps.executeUpdate();

            if(rows > 0) {
                System.out.println("Contact Updated Successfully");
            }
        }
    }

    @Override
    public void deleteContact(int id) throws SQLException {
        // TODO: Delete contact row from database by ID.
        String sql = "DELETE FROM contacts WHERE id=?";

        try (
                Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
                ) {
            ps.setInt(1, id);

            int rows = ps.executeUpdate();
            if(rows > 0) {
                System.out.println("Contact Deleted Successfully");
            }
        }
    }

    @Override
    public void importContactsBatch(List<Contact> contacts) throws SQLException {
        // TODO: Implement transaction-controlled batch insertion
        String sql = """
                INSERT INTO contacts
                (first_name, last_name, phone, email, address)
                VALUES(?,?,?,?,?)
                """;

        Connection conn = null;
        try{
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement ps = conn.prepareStatement(sql);

            for(Contact contact : contacts) {
                ps.setString(1, contact.getFirstName());
                ps.setString(2, contact.getLastName());
                ps.setString(3, contact.getPhone());
                ps.setString(4, contact.getEmail());
                ps.setString(5, contact.getAddress());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            System.out.println("Contact Imported Successfully");
        } catch (SQLException e){
            if(conn != null){
                conn.rollback();
            }
            throw e;
        }
    }
        // 1. Disable auto-commit
        // 2. Iterate list and call addBatch() on PreparedStatement
        // 3. executeBatch()
        // 4. commit() transaction manually
        // 5. In catch-block: rollback() on failure to preserve atomicity
}
