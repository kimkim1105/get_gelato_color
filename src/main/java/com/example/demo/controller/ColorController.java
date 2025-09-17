package com.example.demo.controller;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ColorController {

    @GetMapping("/colors")
    public ResponseEntity<byte[]> getColors(
            @RequestParam String url,
            @RequestParam(defaultValue = "United States") String region
    ) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // chạy headless
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        List<String> colors = new ArrayList<>();

        try {
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // Xử lý chọn region (nếu có popup xuất hiện)
            try {
                WebElement regionButton =
                        driver.findElement(By.xpath("//*[@id=\"product\"]/lib-product-details/gd-fixed-width-container/div/div/div[1]/div/div[2]/gd-product-prices-and-shipment/div/div[1]/div[2]/div/gd-product-minimal-price/div/div/div[2]/div[3]/div/gd-country-selector/ng-select/div/div/div[2]"));

                regionButton.click();

                WebElement regionOption = wait.until(
                        ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(),'" + region + "')]"))
                );
                regionOption.click();
            } catch (Exception e) {
                // Nếu không có popup chọn region thì bỏ qua
                System.out.println("Không tìm thấy popup chọn region, bỏ qua...");
            }

            try {
                WebElement showMore = driver.findElement(By.xpath("//*[@id=\"product\"]/lib-product-details/gd-fixed-width-container/div/div/div[1]/div/div[2]/div[1]/div/div/div/gd-product-control-field[3]/gd-product-control-garment-color/div/div[2]/ui-show-more-button/button"));
                if (showMore.isDisplayed()) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("document.querySelector('.onetrust-pc-dark-filter').remove();");
                    showMore.click();
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                System.out.println("full mã màu");
            }

            // Chờ danh sách màu render (áo)
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".color.ng-star-inserted")));

            List<WebElement> colorElements = driver.findElements(By.cssSelector(".color.ng-star-inserted"));



//            click mở option chọn size
//            WebElement dropdown = wait.until(
//                    ExpectedConditions.elementToBeClickable(By.cssSelector("ng-select[formcontrolname='control']"))
//            );
//            JavascriptExecutor js = (JavascriptExecutor) driver;
//            js.executeScript("document.querySelector('.onetrust-pc-dark-filter').remove();");
//            dropdown.click();
//
//            // Chờ danh sách size render (poster)
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ng-dropdown-panel")));
//
//            List<WebElement> colorElements = driver.findElements(By.cssSelector("ng-dropdown-panel .ng-option"));




//            sp áo pod
            for (WebElement el : colorElements) {
//                String color = el.getAttribute("cy-data");
                String color = el.getAttribute("cy-data");
                if (color == null || color.isEmpty()) {
                    color = el.getText();
                }
                if (color != null && !color.isBlank()) {
                    colors.add(color.trim());
                }
            }


//            sp poster
//            for (WebElement opt : colorElements) {
//                colors.add(opt.getText().trim());
//            }




            // Xuất ra Excel
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Colors");

            int rowNum = 0;
            Row header = sheet.createRow(rowNum++);
            header.createCell(0).setCellValue("Color");

            for (String color : colors) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(color);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            byte[] excelBytes = out.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=colors.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        } finally {
            driver.quit();
        }
    }
}
