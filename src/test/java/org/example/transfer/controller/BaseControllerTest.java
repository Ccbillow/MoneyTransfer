package org.example.transfer.controller;

import org.example.transfer.comm.enums.Currency;
import org.example.transfer.model.Account;
import org.example.transfer.model.FxRate;
import org.example.transfer.params.req.TransferRequest;
import org.example.transfer.params.resp.CommonResponse;
import org.example.transfer.repository.AccountRepository;
import org.example.transfer.repository.FxRateRepository;
import org.example.transfer.repository.TransferLogRepository;
import org.example.transfer.util.JsonUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseControllerTest {

    public final static String DEFAULT_ACCOUNR_PARH = "testdata/accounts_default.json";
    public final static String DEFAULT_RATE_PARH = "testdata/rate_default.json";
    public final static String TRANSFER_URL = "/api/transfer";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected FxRateRepository fxRateRepository;

    @Autowired
    protected TransferLogRepository transferLogRepository;

    /**
     * default setup
     * <p>
     * clean and load data from json
     *
     * @param accountPath
     * @param ratePath
     */
    protected void setup(String accountPath, String ratePath) {
        // del old data
        accountRepository.deleteAllAccountsNative();
        fxRateRepository.deleteAll();
        transferLogRepository.deleteAll();

        // load new data
        List<Account> accounts = JsonUtils.fromPathToObjList(accountPath, Account.class);
        List<FxRate> balances = JsonUtils.fromPathToObjList(ratePath, FxRate.class);

        accountRepository.saveAll(accounts);
        fxRateRepository.saveAll(balances);
    }

    protected CommonResponse<Void> send(Long fromId, Long toId, double amount, Currency currency) throws Exception {
        try {
            TransferRequest request = new TransferRequest();
            request.setFromId(fromId);
            request.setToId(toId);
            request.setAmount(BigDecimal.valueOf(amount));
            request.setTransferCurrency(currency);

            String content = mockMvc.perform(post(TRANSFER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(Objects.requireNonNull(JsonUtils.toJson(request))))
                    .andReturn().getResponse().getContentAsString();
            return JsonUtils.fromJson(content, CommonResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResponse<>();
        }
    }

    protected void verifyBalance(Long accountId, BigDecimal expectedBalances) {
        accountRepository.findById(accountId).ifPresent(account ->
                assertEquals(0, account.getBalance().compareTo(expectedBalances)));
    }

    protected Callable<CommonResponse<Void>> randomTransferTask(Random random) {
        return () -> send(1L, 2L, 1 + random.nextInt(5), Currency.USD);
    }
}