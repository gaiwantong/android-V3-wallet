package info.blockchain.wallet.util;

import android.content.Context;
import android.graphics.Typeface;

public class TypefaceUtil {

    public static int awesome_checkmark = 0xf00c;
    public static int awesome_angle_double_up = 0xf102;
    public static int awesome_angle_double_down = 0xf103;
    public static int awesome_angle_double_left = 0xf100;
    public static int awesome_angle_down = 0xf107;
    public static int awesome_arrow_left = 0xf060;
    public static int awesome_arrow_right = 0xf061;
    public static int awesome_arrow_down = 0xf063;
    public static int awesome_home = 0xf015;
    public static int awesome_pencil_square = 0xf044;
    public static int awesome_comment = 0xf0e5;

    private static Typeface roboto_font = null;
    private static Typeface roboto_boldfont = null;
    private static Typeface roboto_lightfont = null;
    private static Typeface awesome_font = null;
    private static TypefaceUtil instance = null;

    private TypefaceUtil() {
        ;
    }

    public static TypefaceUtil getInstance(Context ctx) {

        if (instance == null) {

            instance = new TypefaceUtil();

            roboto_font = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Regular.ttf");
            roboto_boldfont = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Bold.ttf");
            roboto_lightfont = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Light.ttf");
            awesome_font = Typeface.createFromAsset(ctx.getAssets(), "fontawesome-webfont.ttf");
        }

        return instance;
    }

    public Typeface getRobotoTypeface() {
        return roboto_font;
    }

    public Typeface getRobotoBoldTypeface() {
        return roboto_boldfont;
    }

    public Typeface getRobotoLightTypeface() {
        return roboto_lightfont;
    }

    public Typeface getAwesomeTypeface() {
        return awesome_font;
    }

}
