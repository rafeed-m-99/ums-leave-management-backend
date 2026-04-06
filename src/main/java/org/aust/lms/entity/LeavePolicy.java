package org.aust.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.aust.lms.enums.GenderRestriction;
import org.aust.lms.enums.LeaveApplicantType;
import org.aust.lms.enums.LeaveSalaryType;

@Entity
@Table(name = "leave_policy")
@NoArgsConstructor @AllArgsConstructor @ToString
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    private LeaveApplicantType applicantType;

    @Enumerated(EnumType.STRING)
    private GenderRestriction genderApplicable;

    @Enumerated(EnumType.STRING)
    private LeaveSalaryType salaryType;

    @ManyToOne
    @JoinColumn(name = "balance_cut_from_leave_type_id")
    private LeaveType balanceCutFromLeaveType;

    private Integer minYearsOfService;

    private Integer maxTimesInCareer;

    private Integer maxDurationPerApplication;

    private Integer maxDuration;

    private Integer maxDurationSpecial;

    private Integer maxDurationExtraEL;

    private Boolean isPerYear;

    private Boolean sandwichAllowed;

    private Integer maxSandwichPerYear;

    private Integer maxSandwichDaysPerApplication;

    private Boolean allowedExBDLeave;

    private Boolean conversionAllowed;

    @ManyToOne
    @JoinColumn(name = "conversion_from_leave_type_id")
    private LeaveType conversionFromLeaveType;

    private Integer conversionRatioFrom;

    private Integer conversionRatioTo;

    private Integer maxConvertibleInCareer;

    public Long getId() {
        return id;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LeaveApplicantType getApplicantType() {
        return applicantType;
    }

    public GenderRestriction getGenderApplicable() {
        return genderApplicable;
    }

    public LeaveSalaryType getSalaryType() {
        return salaryType;
    }

    public LeaveType getBalanceCutFromLeaveType() {
        return balanceCutFromLeaveType;
    }

    public Integer getMinYearsOfService() {
        return minYearsOfService;
    }

    public Integer getMaxTimesInCareer() {
        return maxTimesInCareer;
    }

    public Integer getMaxDurationPerApplication() {
        return maxDurationPerApplication;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public Integer getMaxDurationSpecial() {
        return maxDurationSpecial;
    }

    public Integer getMaxDurationExtraEL() {
        return maxDurationExtraEL;
    }

    public Boolean getPerYear() {
        return isPerYear;
    }

    public Boolean getSandwichAllowed() {
        return sandwichAllowed;
    }

    public Integer getMaxSandwichPerYear() {
        return maxSandwichPerYear;
    }

    public Integer getMaxSandwichDaysPerApplication() {
        return maxSandwichDaysPerApplication;
    }

    public Boolean getAllowedExBDLeave() {
        return allowedExBDLeave;
    }

    public Boolean getConversionAllowed() {
        return conversionAllowed;
    }

    public LeaveType getConversionFromLeaveType() {
        return conversionFromLeaveType;
    }

    public Integer getConversionRatioFrom() {
        return conversionRatioFrom;
    }

    public Integer getConversionRatioTo() {
        return conversionRatioTo;
    }

    public Integer getMaxConvertibleInCareer() {
        return maxConvertibleInCareer;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public void setApplicantType(LeaveApplicantType applicantType) {
        this.applicantType = applicantType;
    }

    public void setGenderApplicable(GenderRestriction genderApplicable) {
        this.genderApplicable = genderApplicable;
    }

    public void setSalaryType(LeaveSalaryType salaryType) {
        this.salaryType = salaryType;
    }

    public void setBalanceCutFromLeaveType(LeaveType balanceCutFromLeaveType) {
        this.balanceCutFromLeaveType = balanceCutFromLeaveType;
    }

    public void setMinYearsOfService(Integer minYearsOfService) {
        this.minYearsOfService = minYearsOfService;
    }

    public void setMaxTimesInCareer(Integer maxTimesInCareer) {
        this.maxTimesInCareer = maxTimesInCareer;
    }

    public void setMaxDurationPerApplication(Integer maxDurationPerApplication) {
        this.maxDurationPerApplication = maxDurationPerApplication;
    }

    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public void setMaxDurationSpecial(Integer maxDurationSpecial) {
        this.maxDurationSpecial = maxDurationSpecial;
    }

    public void setMaxDurationExtraEL(Integer maxDurationExtraEL) {
        this.maxDurationExtraEL = maxDurationExtraEL;
    }

    public void setPerYear(Boolean perYear) {
        isPerYear = perYear;
    }

    public void setSandwichAllowed(Boolean sandwichAllowed) {
        this.sandwichAllowed = sandwichAllowed;
    }

    public void setMaxSandwichPerYear(Integer maxSandwichPerYear) {
        this.maxSandwichPerYear = maxSandwichPerYear;
    }

    public void setMaxSandwichDaysPerApplication(Integer maxSandwichDaysPerApplication) {
        this.maxSandwichDaysPerApplication = maxSandwichDaysPerApplication;
    }

    public void setAllowedExBDLeave(Boolean allowedExBDLeave) {
        this.allowedExBDLeave = allowedExBDLeave;
    }

    public void setConversionAllowed(Boolean conversionAllowed) {
        this.conversionAllowed = conversionAllowed;
    }

    public void setConversionFromLeaveType(LeaveType conversionFromLeaveType) {
        this.conversionFromLeaveType = conversionFromLeaveType;
    }

    public void setConversionRatioFrom(Integer conversionRatioFrom) {
        this.conversionRatioFrom = conversionRatioFrom;
    }

    public void setConversionRatioTo(Integer conversionRatioTo) {
        this.conversionRatioTo = conversionRatioTo;
    }

    public void setMaxConvertibleInCareer(Integer maxConvertibleInCareer) {
        this.maxConvertibleInCareer = maxConvertibleInCareer;
    }
}
