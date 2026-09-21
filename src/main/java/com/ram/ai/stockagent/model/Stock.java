package com.ram.ai.stockagent.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    private String sector;

    private Double price;

    private Double open;

    private Double high;

    private Double low;

    private Double previousClose;

    private Double changePercent;

    private Long volume;

    private Long averageVolume;

    public Stock() {
    }

    public Stock(
            String symbol,
            String companyName,
            String sector,
            Double price,
            Double open,
            Double high,
            Double low,
            Double previousClose,
            Double changePercent,
            Long volume,
            Long averageVolume) {

        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.price = price;
        this.open = open;
        this.high = high;
        this.low = low;
        this.previousClose = previousClose;
        this.changePercent = changePercent;
        this.volume = volume;
        this.averageVolume = averageVolume;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getAverageVolume() {
        return averageVolume;
    }

    public void setAverageVolume(Long averageVolume) {
        this.averageVolume = averageVolume;
    }
}