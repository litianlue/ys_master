package com.yeespec.microscope.utils.sql;

/**
 * Created by Mr.Wen on 2016/7/8.
 *
 * @author Mr.Wen
 * @company YeeSpec
 * @time 2016/7/8 19:06
 */
public class ConfigurationParameter {

    //配置参数的实体 :用来保存当前系统的配置参数 :

    //参数设置 : Preferences_parameter
    /**
     * _id          主键 ;
     * multiple     倍数 ;
     * stimulated_light  激发光 ;(varchar)
     * tinting      着色 ;
     * sensitivity  感光度ISO ;
     * brightness   亮度 ;
     * gamma        焦点
     */

    private long id = 0;    //主键 ;
    private String user=""; //用户名
    private String multiple = "";    //倍数 ;
    private int stimulatedLight = 0; //激发光
    private int tinting = 0;    //着色 ;
    private int sensitivity = 0; //感光度ISO ;
    private int brightness = 0;    //亮度 ;
    private String tintingString = "";    //选择的染色 ;
    private int gamma=0;//焦点

    public ConfigurationParameter() {
    }

    public ConfigurationParameter(long id, String user,String multiple,
                                  int stimulatedLight, int tinting,
                                  int sensitivity, int brightness,
                                  String tintingString,int gamma) {
        this.id = id;
        this.user = user;
        this.multiple = multiple;
        this.stimulatedLight = stimulatedLight;
        this.tinting = tinting;
        this.sensitivity = sensitivity;
        this.brightness = brightness;
        this.tintingString = tintingString;
        this.gamma = gamma;
    }

   /* public ConfigurationParameter(long id, String multiple, String stimulatedLight, int tinting, int sensitivity, int brightness) {
        this.id = id;
        this.multiple = multiple;
        this.stimulatedLight = stimulatedLight;
        this.tinting = tinting;
        this.sensitivity = sensitivity;
        this.brightness = brightness;

    }*/

  /*  public ConfigurationParameter(String multiple, String stimulatedLight, int tinting, int sensitivity, int brightness) {
        this.multiple = multiple;
        this.stimulatedLight = stimulatedLight;
        this.tinting = tinting;
        this.sensitivity = sensitivity;
        this.brightness = brightness;
    }
*/
    @Override
    public String toString() {
        return "ConfigurationParameter{" +
                "id=" + id +
                ", user='" + user + '\'' +
                ", multiple='" + multiple + '\'' +
                ", stimulatedLight='" + stimulatedLight + '\'' +
                ", tinting=" + tinting +
                ", sensitivity=" + sensitivity +
                ", brightness=" + brightness +
                ", tintingString=" + tintingString +
                ", gamma=" + gamma +
                '}';
    }

    public int getGamma() {
        return gamma;
    }

    public void setGamma(int gamma) {
        this.gamma = gamma;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMultiple() {
        return multiple;
    }

    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }

    public int getStimulatedLight() {
        return stimulatedLight;
    }

    public void setStimulatedLight(int stimulatedLight) {
        this.stimulatedLight = stimulatedLight;
    }

    public int getTinting() {
        return tinting;
    }

    public void setTinting(int tinting) {
        this.tinting = tinting;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public String getTintingString() {
        return tintingString;
    }

    public void setTintingString(String tintingString) {
        this.tintingString = tintingString;
    }
}
