package main.java.uptc.model;
public enum Estado {
    TODO("Pendiente"),
    IN_PROGRESS("En curso"),
    DONE("Terminada");

    private final String value;

    Estado(String value)
    {
        this.value = value;
    }

    public String getValue(){
        return value;
    }

    @Override 
    public String toString(){
        return value;
    }
}
