package com.zhenxi.Superappium.xmodel.basic;

import com.zhenxi.Superappium.SuperAppium;
import com.zhenxi.Superappium.ViewImage;
import com.zhenxi.Superappium.xmodel.ValueGetter;
import android.widget.ImageView;
import de.robv.android.xposed.XposedHelpers;


public class ImageUriGetter implements ValueGetter {
    @Override
    public Object get(ViewImage viewImage) {
        return XposedHelpers.getObjectField(viewImage.getOriginView(), "mUri");
    }

    @Override
    public String attr() {
        return SuperAppium.mUri;
    }

    @Override
    public boolean support(Class type) {
        return ImageView.class.isAssignableFrom(type);
    }
}
