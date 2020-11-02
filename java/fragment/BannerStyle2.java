package com.themescoder.androidecommerce.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.DefaultSliderView;
import com.daimajia.slider.library.Transformers.BaseTransformer;
import com.google.android.material.tabs.TabLayout;
import com.themescoder.androidecommerce.R;
import com.themescoder.androidecommerce.adapters.BannerStyle2Adapter;
import com.themescoder.androidecommerce.constant.ConstantValues;
import com.themescoder.androidecommerce.customs.BannerStyle2ViewPager;
import com.themescoder.androidecommerce.models.banner_model.BannerDetails;
import com.themescoder.androidecommerce.models.category_model.CategoryDetails;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class BannerStyle2 extends Fragment {


    View rootView;
    BannerStyle2ViewPager slidingBannerViewPager;
    BannerStyle2Adapter adapter;
    TabLayout bannerDots;

    FragmentManager fragmentManager;

    List<BannerDetails> bannerImages = new ArrayList<>();
    List<CategoryDetails> allCategoriesList = new ArrayList<>();
    public static final int DELAY = 5000;
    int currentPage = 0;
    private Handler handler = new Handler();
    private Runnable runnableCode = null;

    public BannerStyle2(List<BannerDetails> bannerImages, List<CategoryDetails> allCategoriesList) {
        this.bannerImages = bannerImages;
        this.allCategoriesList = allCategoriesList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.banner_style_2, container, false);


        slidingBannerViewPager = rootView.findViewById(R.id.slidingBanner);
        bannerDots = rootView.findViewById(R.id.bannerDots);

        setupSlidingBanner();
        addFragments(bannerImages);
        return rootView;
    }


    private void setupSlidingBanner() {

        ViewGroup.LayoutParams layoutParams = slidingBannerViewPager.getLayoutParams();
        layoutParams.height = Math.round((((float) ((Resources.getSystem().getDisplayMetrics().widthPixels)) * 1.0f) / 2.0f) + 250);
        slidingBannerViewPager.setLayoutParams(layoutParams);

        adapter = new BannerStyle2Adapter(getChildFragmentManager());

        slidingBannerViewPager.setAdapter(adapter);

    }

    private void addFragments(List<BannerDetails> banners) {
        adapter.clear();
        for (int i = 0; i < banners.size(); i++) {
            BannerDetails banner = banners.get(i);
            BannerItemFragment fragment = new BannerItemFragment(banner.getImage());
            adapter.addFrag(fragment, banner.getTitle());
        }

        invalidateBannerDots();

        startAutoSlider(banners.size());
    }

    private void invalidateBannerDots() {
        for (int i = 0; i < bannerDots.getTabCount(); i++) {
            View tab = ((ViewGroup) bannerDots.getChildAt(0)).getChildAt(i);
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tab.getLayoutParams();
            p.setMargins(-10, 0, -10, 0);
            tab.requestLayout();
        }

        LinearLayout tabStrip = ((LinearLayout) bannerDots.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setClickable(false);
        }
    }

    private void startAutoSlider(final int i) {
        this.runnableCode = new Runnable() {
            public void run() {
                int currentItem = slidingBannerViewPager.getCurrentItem() + 1;
                if (currentItem >= i) {
                    currentItem = 0;
                }
                slidingBannerViewPager.setCurrentItem(currentItem, true);
                handler.postDelayed(runnableCode, DELAY);
            }
        };
        this.handler.postDelayed(this.runnableCode, DELAY);
    }

}
