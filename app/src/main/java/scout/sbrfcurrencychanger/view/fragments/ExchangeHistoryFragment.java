package scout.sbrfcurrencychanger.view.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.Exchange;
import scout.sbrfcurrencychanger.view.adapters.ExchangeAdapter;

/**
 * Фрагмент "История"
 */
public class ExchangeHistoryFragment extends Fragment {

    private Calendar mDateFrom;
    private ExchangeAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchange_history, container, false);
        Repository.initialize(view.getContext());
        mDateFrom = Repository.getToday();

        ListView mListView = (ListView) view.findViewById(R.id.exchangeFragment_lvExchanges);
        mAdapter = new ExchangeAdapter(view.getContext(), R.layout.listview_exchange_list_row, new ArrayList<Exchange>());
        mListView.setAdapter(mAdapter);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                if (i + i2 == i3) {
                    Date dateTo;
                    if (i3 == 0) {
                        Calendar dateToCalendar = Repository.getToday();
                        dateTo = dateToCalendar.getTime();
                    } else {
                        mDateFrom.add(Calendar.DAY_OF_YEAR, -1);
                        dateTo = mDateFrom.getTime();
                    }
                    mDateFrom.add(Calendar.DAY_OF_YEAR, -6);
                    Exchange[] exchanges = Repository.getExchanges(mDateFrom.getTime(), getEndOfDate(dateTo));
                    if (exchanges.length > 0) {
                        mAdapter.addAll(exchanges);
                    }
                }
            }
        });
        return view;
    }

    /**
     * Конец дня
     * @param date Исходная дата
     */
    private static Date getEndOfDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.HOUR, 23);
        return calendar.getTime();
    }

}
