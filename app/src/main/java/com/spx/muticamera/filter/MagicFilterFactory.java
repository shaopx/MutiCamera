package com.spx.muticamera.filter;

import com.spx.muticamera.filter.magic.MagicAntiqueFilter;
import com.spx.muticamera.filter.magic.MagicBrannanFilter;
import com.spx.muticamera.filter.magic.MagicCoolFilter;
import com.spx.muticamera.filter.magic.MagicFreudFilter;
import com.spx.muticamera.filter.magic.MagicHefeFilter;
import com.spx.muticamera.filter.magic.MagicHudsonFilter;
import com.spx.muticamera.filter.magic.MagicInkwellFilter;
import com.spx.muticamera.filter.magic.MagicN1977Filter;
import com.spx.muticamera.filter.magic.MagicNashvilleFilter;

public class MagicFilterFactory {

    private static MagicFilterType filterType = MagicFilterType.NONE;

    public static GPUImageFilter initFilters(MagicFilterType type) {
        if (type == null) {
            return null;
        }
        filterType = type;
        switch (type) {
            case ANTIQUE:
                return new MagicAntiqueFilter();
            case BRANNAN:
                return new MagicBrannanFilter();
            case FREUD:
                return new MagicFreudFilter();
            case HEFE:
                return new MagicHefeFilter();
            case HUDSON:
                return new MagicHudsonFilter();
            case INKWELL:
                return new MagicInkwellFilter();
            case N1977:
                return new MagicN1977Filter();
            case NASHVILLE:
                return new MagicNashvilleFilter();
            case COOL:
                return new MagicCoolFilter();
            case WARM:
                return new MagicWarmFilter();
            default:
                return null;
        }
    }

    public MagicFilterType getCurrentFilterType() {
        return filterType;
    }

    private static class MagicWarmFilter extends GPUImageFilter {
    }
}
