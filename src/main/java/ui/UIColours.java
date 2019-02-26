package ui;

public enum UIColours {
  GREEN("#199d6e"), QUIT_RED("#ff0202"), RED("#ff436d"),
  WHITE("#ffffff");

  private String hex;

  UIColours(String hexvalue) {
    this.hex = hexvalue;
  }

  public String getHex() {
    return this.hex;
  }
}