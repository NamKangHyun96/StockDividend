package com.zerobase.scheduler;

import com.zerobase.model.Company;
import com.zerobase.model.ScrapedResult;
import com.zerobase.model.constants.CacheKey;
import com.zerobase.persist.CompanyRepository;
import com.zerobase.persist.DividendRepository;
import com.zerobase.persist.entity.CompanyEntity;
import com.zerobase.persist.entity.DividendEntity;
import com.zerobase.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

    // 일정 주기마다 수행
    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");
        // 저장된 회사 목록을 조회
        List<CompanyEntity> companies = this.companyRepository.findAll();

        // 회사마다 배당금 정보를 새로 스크랩핑
        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName());
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(
                    new Company(
                            company.getTicker(),
                            company.getName()
                    )
            );

        //스크래핑한 배당금 정보 중 데이터베이스에 없는 값은 저장
            scrapedResult.getDividends().stream()
                    // dividend model 을 dividend entity 로 mapping
                    .map(e -> new DividendEntity(company.getId(), e))
                    // element 를 하나씩 dividend repository 에 insert
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.
                                existsByCompanyIdAndDate(
                                        e.getCompanyId(),
                                        e.getDate()
                                );
                        if (!exists) {
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    }
                    );

            // 연속적으로 스크래핑 대상 사이터 서버에 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
