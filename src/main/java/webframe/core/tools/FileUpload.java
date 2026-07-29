package webframe.core.tools;

/**
 * Encapsule un fichier uploadé depuis un formulaire HTML (multipart/form-data).
 */
public class FileUpload {
    private String nom;
    private byte[] bytes;

    public FileUpload() {
    }

    public FileUpload(String nom, byte[] bytes) {
        this.nom = nom;
        this.bytes = bytes;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
