package edu.bjfu.onlinesm.model;

import java.math.BigDecimal;

/**
 * 稿件项目资助（多条）：名称 / 级别 / 资助金额。
 *
 * 对应表：dbo.ManuscriptFundings
 */
public class ManuscriptFunding {

    private Integer fundingId;
    private Integer manuscriptId;
    private String fundingName;
    private String fundingLevel;
    private BigDecimal fundingAmount;

    public Integer getFundingId() {
        return fundingId;
    }

    public void setFundingId(Integer fundingId) {
        this.fundingId = fundingId;
    }

    public Integer getManuscriptId() {
        return manuscriptId;
    }

    public void setManuscriptId(Integer manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getFundingName() {
        return fundingName;
    }

    public void setFundingName(String fundingName) {
        this.fundingName = fundingName;
    }

    public String getFundingLevel() {
        return fundingLevel;
    }

    public void setFundingLevel(String fundingLevel) {
        this.fundingLevel = fundingLevel;
    }

    public BigDecimal getFundingAmount() {
        return fundingAmount;
    }

    public void setFundingAmount(BigDecimal fundingAmount) {
        this.fundingAmount = fundingAmount;
    }
}
