package ru.gdgkazan.simpleweather.screen.weatherlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;
import ru.gdgkazan.simpleweather.screen.general.SimpleDividerItemDecoration;
import ru.gdgkazan.simpleweather.screen.weather.RetrofitWeatherLoader;
import ru.gdgkazan.simpleweather.screen.weather.WeatherActivity;

/**
 * @author Artur Vasilov
 */
public class WeatherListActivity extends AppCompatActivity implements CitiesAdapter.OnItemClick {

    private static final String TAG = WeatherListActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty)
    View mEmptyView;

    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private CitiesAdapter mAdapter;

    private LoadingView mLoadingView;

    private String mCityName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, false));
        mAdapter = new CitiesAdapter(getInitialCities(), this);
        mRecyclerView.setAdapter(mAdapter);
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(false);
            loadWeather(true);
        });

        /**
         * TODO : task
         *
         * 1) Load all cities forecast using one or multiple loaders
         * 2) Try to run these requests as most parallel as possible
         * or better do as less requests as possible
         * 3) Show loading indicator during loading process
         * 4) Allow to update forecasts with SwipeRefreshLayout
         * 5) Handle configuration changes
         *
         * Note that for the start point you only have cities names, not ids,
         * so you can't load multiple cities in one request.
         *
         * But you should think how to manage this case. I suggest you to start from reading docs mindfully.
         */
        loadWeather(false);
    }

    @Override
    public void onItemClick(@NonNull City city) {
        startActivity(WeatherActivity.makeIntent(this, city.getName()));
    }

    @NonNull
    private List<City> getInitialCities() {
        List<City> cities = new ArrayList<>();
        String[] initialCities = getResources().getStringArray(R.array.initial_cities);
        for (String city : initialCities) {
            cities.add(new City(city));
        }
        return cities;
    }

    private void loadWeather(boolean restart) {
        mLoadingView.showLoadingIndicator();
        LoaderManager.LoaderCallbacks<City> callbacks = new WeatherListActivity.WeatherCallbacks();
        for (int i = 0; i < getInitialCities().size(); i++) {
            mCityName = getInitialCities().get(i).getName();
            if (restart) {
                getSupportLoaderManager().restartLoader(R.id.weather_loader_id + i, Bundle.EMPTY, callbacks);
            } else {
                getSupportLoaderManager().initLoader(R.id.weather_loader_id + i, Bundle.EMPTY, callbacks);
            }
        }
    }

    private void showWeather(@Nullable City city) {
        if (city == null || city.getMain() == null || city.getWeather() == null
                || city.getWind() == null) {
            showError();
            return;
        }
        Log.d(TAG, "City " + city.getName() + ", temperature " + city.getMain().getTemp());
        mLoadingView.hideLoadingIndicator();
    }

    private void showError() {
        Log.d(TAG, "loading error for " + mCityName);
        mLoadingView.hideLoadingIndicator();
    }

    private class WeatherCallbacks implements LoaderManager.LoaderCallbacks<City> {

        @Override
        public Loader<City> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader " + id);
            return new RetrofitWeatherLoader(WeatherListActivity.this, mCityName);
        }

        @Override
        public void onLoadFinished(Loader<City> loader, City city) {
            Log.d(TAG, "onLoadFinished " + loader.getId());
            showWeather(city);
        }

        @Override
        public void onLoaderReset(Loader<City> loader) {
            Log.d(TAG, "onLoaderReset " + loader.getId());
            mCityName = null;
            // Do nothing
        }
    }
}
