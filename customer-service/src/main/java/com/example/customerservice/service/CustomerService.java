package com.example.customerservice.service;

import com.example.customerservice.entities.Customer;
import com.example.customerservice.repository.CustomerRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    @McpTool(description="Get All customers ")
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    @McpTool(description="Find a customer by Id ")
    public Customer findCustomerById(@McpToolParam(description = "the customer id") Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer Not Found"));
    }
    @McpTool(description="Save new customers ")
    public Customer saveCustomer(@McpToolParam(description = "the customer to save(name,email ")Customer customer) {
        return customerRepository.save(customer);
    }
}
