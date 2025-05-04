package org.example.accounts.service.impl;

import lombok.AllArgsConstructor;
import org.example.accounts.constants.AccountsConstants;
import org.example.accounts.dto.AccountsDto;
import org.example.accounts.dto.AccountsMsgDto;
import org.example.accounts.dto.CustomerDto;
import org.example.accounts.entity.Accounts;
import org.example.accounts.entity.Customer;
import org.example.accounts.exception.CustomerAlreadyExistsException;
import org.example.accounts.exception.ResourceNotFoundException;
import org.example.accounts.mapper.AccountsMapper;
import org.example.accounts.mapper.CustomerMapper;
import org.example.accounts.repository.AccountsRepository;
import org.example.accounts.repository.CustomerRepository;
import org.example.accounts.service.IAccountsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsServiceImpl.class);


    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private final StreamBridge streamBridge;           // Spring Cloud Stream component for message publishing

    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber());
        if (optionalCustomer.isPresent()) {
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "
                                                     + customerDto.getMobileNumber());
        }

//        customer.setCreatedBy("me");
//        customer.setCreatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);
        Accounts savedAccount = accountsRepository.save(createNewAccount(savedCustomer));
        sendCommunication(savedAccount, savedCustomer);

    }

    /**
     * Sends communication about the newly created account via Spring Cloud Stream
     * @param account The newly created account
     * @param customer The customer associated with the account
     */
    private void sendCommunication(Accounts account, Customer customer) {
        // Create message DTO with relevant account and customer information
        var accountsMsgDto = new AccountsMsgDto(
                account.getAccountNumber(),
                customer.getName(),
                customer.getEmail(),
                customer.getMobileNumber()
        );

        log.info("Sending Communication request for the details: {}", accountsMsgDto);

        // Send message to the configured output channel
        // The binding name "sendCommunication-out-0" matches the configuration in application.yml
        var result = streamBridge.send("sendCommunication-out-0", accountsMsgDto); // mentionned in application.yml (spring.cloud.stream.bindings.sendCommunication-out-0.destination=send-communication)

        log.info("Is the Communication request successfully triggered ? : {}", result);
    }

    /**
     * @param customer - Customer Object
     * @return the new account details
     */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1_000_000_000L + new Random().nextInt(900_000_000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);

//        newAccount.setCreatedBy(customer.getCreatedBy());
//        newAccount.setCreatedAt(customer.getCreatedAt());
        return newAccount;
    }

    /**
     * @param mobileNumber - Input Mobile Number
     * @return Accounts Details based on a given mobileNumber
     */
    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
        return customerDto;
    }


    /**
     * @param customerDto - CustomerDto Object
     * @return boolean indicating if the update of Account details is successful or not
     */
    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if(accountsDto !=null ){
            Accounts accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
            );
            AccountsMapper.mapToAccounts(accountsDto, accounts);
            accounts = accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepository.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerID", customerId.toString())
            );
            CustomerMapper.mapToCustomer(customerDto,customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return  isUpdated;
    }

    /**
     * @param mobileNumber - Input Mobile Number
     * @return boolean indicating if the delete of Account details is successful or not
     */
    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
        return true;
    }

    /**
     * @param accountNumber - Long
     * @return boolean indicating if the update of communication status is successful or not
     */
    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated = false;
        if(accountNumber !=null ){
            Accounts accounts = accountsRepository.findById(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountNumber.toString())
            );
            accounts.setCommunicationSw(true);
            accountsRepository.save(accounts);
            isUpdated = true;
        }
        return  isUpdated;
    }

}
