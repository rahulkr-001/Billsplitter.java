package billsplitter;

// Module 2: Abstract class & method
public abstract class Bill {
    protected String title;
    protected double totalAmount;
    protected String[] members; // Module 1: Arrays

    public Bill(String title, double totalAmount, String[] members) throws InvalidBillException {
        if (totalAmount <= 0) {
            throw new InvalidBillException("Bill amount must be greater than zero!");
        }
        if (members == null || members.length == 0) {
            throw new InvalidBillException("At least one person is required to split a bill!");
        }
        this.title = title;
        this.totalAmount = totalAmount;
        this.members = members;
    }

    // Abstract method to be overridden by subclasses
    public abstract double calculateSharePerPerson();

    public String getTitle() {
        return title;
    }

    public String[] getMembers() {
        return members;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}

// Subclass 1: Simple Equal Split
class EqualBill extends Bill {
    public EqualBill(String title, double totalAmount, String[] members) throws InvalidBillException {
        super(title, totalAmount, members); // Module 2: super keyword
    }

    @Override
    public double calculateSharePerPerson() {
        return totalAmount / members.length;
    }

    public double getTotalWithTax() {
        return totalAmount;
    }
}

// Subclass 2: Split with GST/Service Charge included
class TaxedBill extends Bill {
    private double taxPercent;

    public TaxedBill(String title, double totalAmount, String[] members, double taxPercent) throws InvalidBillException {
        super(title, totalAmount, members);
        this.taxPercent = taxPercent;
    }

    @Override
    public double calculateSharePerPerson() {
        double finalTotal = totalAmount + (totalAmount * taxPercent / 100.0);
        return finalTotal / members.length;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public double getTotalWithTax() {
        return totalAmount + (totalAmount * taxPercent / 100.0);
    }
}
