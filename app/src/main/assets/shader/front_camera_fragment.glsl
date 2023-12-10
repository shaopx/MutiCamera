#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 textureCoordinate;
uniform samplerExternalOES vTexture;
uniform samplerExternalOES vTexture2;
uniform vec2 u_resolution;
uniform vec2 u_movement;

void main() {

//    vec2 st = (gl_FragCoord.xy-u_movement)/u_resolution;
//    float ratio = 4./3.;
//    float radiusWidthRatio = 0.1;
//    float R = 1.0 * radiusWidthRatio;
//    // a. The DISTANCE from the pixel to the center
//    //    pct = distance(st,vec2(0.1, 1.0-0.1/1.77778));
//    //    if (pct > 0.5) {
//    //        discard;
//    //    }
//    float topLeftCenterY = 1.0 - radiusWidthRatio / ratio;
//    float topLeftCenterX = R;
//    if (st.x < topLeftCenterX && st.y > topLeftCenterY) {
////        discard;
//        float _x = topLeftCenterX - st.x;
//        if (st.y > topLeftCenterY + sqrt(R*R - _x* _x) / ratio) {
//            discard;
//        }
//    }
//
//    float topRightCenterY = 1.0 - radiusWidthRatio / ratio;
//    float topRightCenterX = 1.0 - R;
//    if (st.x > topRightCenterX && st.y > topRightCenterY) {
////        discard;
//        float _x = st.x-topRightCenterX;
//        if (st.y > topRightCenterY + sqrt(R*R - _x* _x) / ratio) {
//            discard;
//        }
//    }
//
//    float bottomLeftCenterY = radiusWidthRatio/ratio;
//    float bottomLeftCenterX = R;
//    if (st.x < bottomLeftCenterX && st.y < bottomLeftCenterY) {
////        discard;
//        float _x = bottomLeftCenterX - st.x;
//        if (st.y < bottomLeftCenterY - sqrt(R*R - _x* _x) / ratio) {
//            discard;
//        }
//    }
//
//    float bottomRightCenterY = radiusWidthRatio/ratio;
//    float bottomRightCenterX = 1.0 - R;
//    if (st.x > bottomRightCenterX && st.y < bottomRightCenterY) {
////        discard;
//        float _x =st.x-bottomRightCenterX;
//        if (st.y < bottomRightCenterY - sqrt(R*R - _x* _x) / ratio) {
//            discard;
//        }
//    }


    gl_FragColor = texture2D(vTexture, textureCoordinate);
}
