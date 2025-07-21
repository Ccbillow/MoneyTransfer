package org.example.transfer.service;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.comm.enums.ExceptionEnum;
import org.example.transfer.exception.BusinessException;
import org.example.transfer.model.Account;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.FxRateRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * transfer service test
 */
public class TransferServiceTest extends BaseServiceTest {
    @Autowired
    private TransferService transferService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private FxRateRepository fxRateRepository;

    @MockBean
    private TransferLogRepository transferLogRepository;


    @BeforeEach
    public void init() {
        when(transferLogRepository.save(any())).thenReturn(null);
    }

    @Test
    public void testTransferSameUser_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(1L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.PARAM_ILLEGAL.getErrorCode());
        assertEquals(ex.getErrorMsg(), "same account transfer not allowed");
    }

    @Test
    public void testTransferFromNotExist_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(new ArrayList<>());
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.USER_NOT_EXIST.getErrorCode());
        assertEquals(ex.getErrorMsg(), "from account not exist");
    }

    @Test
    public void testTransferToNotExist_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.ONE);

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ex.getErrorCode(), ExceptionEnum.USER_NOT_EXIST.getErrorCode());
        assertEquals(ex.getErrorMsg(), "to account not exist");
    }

    @Test
    public void testTransferFromCurrencyNotMatch_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.AUD);
        request.setAmount(BigDecimal.ONE);

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.JPN);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );
        assertEquals(ex.getErrorCode(), ExceptionEnum.PARAM_ILLEGAL.getErrorCode());
        assertEquals(ex.getErrorMsg(), "Sender must use base currency.");
    }

    @Test
    public void testTransferSameCurrencyInsufficientBalance_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(1000));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.USD);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ExceptionEnum.MONEY_TRANSFER_ERROR.getErrorCode(), ex.getErrorCode());
        assertEquals("Insufficient balance", ex.getErrorMsg());
    }

    @Test
    public void testTransferSameCurrency_Success() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.USD);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));
        when(accountRepository.save(any())).thenReturn(null);

        transferService.transfer(request);

        assertEquals(0, BigDecimal.valueOf(949.50).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(550).compareTo(to.getBalance()));
    }

    @Test
    public void testTransferDiffCurrency_Fail() {
        TransferRequest request = new TransferRequest();
        request.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        request.setFromId(1L);
        request.setToId(2L);
        request.setTransferCurrency(Currency.USD);
        request.setAmount(BigDecimal.valueOf(50));

        Account from = new Account();
        from.setId(1L);
        from.setName("Alice");
        from.setCurrency(Currency.USD);
        from.setBalance(BigDecimal.valueOf(1000));

        Account to = new Account();
        to.setId(2L);
        to.setName("Bob");
        to.setCurrency(Currency.JPN);
        to.setBalance(BigDecimal.valueOf(500));

        when(accountRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(from, to));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> transferService.transfer(request)
        );

        assertEquals(ExceptionEnum.TRANSFER_TYPE_NOT_SUPPORT.getErrorCode(), ex.getErrorCode());
        assertTrue(ex.getErrorMsg().contains("not support transfer type: DIFFERENT"));
    }
}
