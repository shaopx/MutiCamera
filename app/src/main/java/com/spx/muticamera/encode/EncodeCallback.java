package com.spx.muticamera.encode;


import com.spx.muticamera.video.RecordFile;

import java.util.List;

public interface EncodeCallback {
    void onFinishEncode(List<RecordFile> paths);
}