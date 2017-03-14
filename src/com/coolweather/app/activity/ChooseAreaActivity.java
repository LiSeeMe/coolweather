package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.Country;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {
	/**
	 * ��ǵ�ǰ��ʡ��ҳ��
	 */
	public static final int LEVEL_PROVIDER = 0;
	/**
	 * ��ǵ�ǰ���м�ҳ��
	 */
	public static final int LEVEL_CITY = 1;
	/**
	 * ��ǵ�ǰ ���ؼ�ҳ��
	 */
	public static final int LEVEL_COUNTY = 2;
	/**
	 * ���ضԻ���
	 */
	private ProgressDialog progressDialog;
	/**
	 * ����
	 */
	private TextView titleText;
	/**
	 * ����չ��ʡ�����б�
	 */
	private ListView listView;
	/**
	 * ������
	 */
	private ArrayAdapter<String> adapter;
	/**
	 * �������ݿ�Ĺ�����
	 */
	private CoolWeatherDB coolWeatherDB;
	/**
	 * ����Listview������
	 */
	private List<String> dataList = new ArrayList<String>();
	/**
	 * ʡ�б�
	 */
	private List<Province> provincesList;
	/**
	 * ���б�
	 */
	private List<City> cityList;
	/**
	 * ���б�
	 */
	private List<Country> countyList;
	/**
	 * ѡ��ʡ��
	 */
	private Province selectedProvince;
	/**
	 * ѡ�г���
	 */
	private City selectedCity;
	/**
	 * ��ǰѡ�м���
	 */
	private int currentLevel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.choose_area);
		initView();

	}

	private void initView() {
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		Log.v("TAG", "======>>>" + dataList.size());
		coolWeatherDB = CoolWeatherDB.getInstance(this);

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				if (currentLevel == LEVEL_PROVIDER) {
					selectedProvince = provincesList.get(index);
					queryCities();
				} else if (currentLevel == LEVEL_CITY) {

					selectedCity = cityList.get(index);
					queryCounties();

				}
			}

		});
		queryProvinces();// ����ʡ������
	}

	/**
	 * ��ѯȫ�����е�ʡ�����ȴ����ݿ��в�ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ
	 */
	private void queryProvinces() {

		provincesList = coolWeatherDB.loadProvince();
		if (provincesList.size() > 0) {
			dataList.clear();
			for (Province province : provincesList) {
				dataList.add(province.getProvinceName());

			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVIDER;
		} else {
			// �ӷ������ϲ�ѯ���е�ʡ
			queryFromServer(null, "province");
		}
	}

	/**
	 * ��ѯѡ��ʡ�����еõ��У����ȴ����ݿ��ѯ�����û�в鵽���ٴӷ������ϲ�ѯ
	 */
	private void queryCities() {
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if (cityList.size() > 0) {
			dataList.clear();
			for (City city : cityList) {
				dataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(selectedProvince.getProvinceCode(), "city");
		}
	}

	/**
	 * ��ѯѡ���������е��أ����ȴ����ݿ��ѯ�����û�в�ѯ������ȥ��������ѯ
	 */
	private void queryCounties() {

		countyList = coolWeatherDB.loadCountries(selectedCity.getId());
		if (countyList.size() > 0) {
			dataList.clear();
			for (Country country : countyList) {
				dataList.add(country.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(selectedCity.getCityCode(), "county");
		}

	}

	private void queryFromServer(final String code, final String type) {

		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}

		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

			@Override
			public void onFinish(String respone) {

				boolean result = false;
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(coolWeatherDB,
							respone);
				} else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(coolWeatherDB,
							respone, selectedProvince.getId());
				} else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(coolWeatherDB,
							respone, selectedCity.getId());
				}

				if (result) {
					// ͨ��runOnUiThread()�����ص����̴߳����߼�
					runOnUiThread(new Runnable() {

						@Override
						public void run() {

							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});

				}
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ�ܣ�",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * ��ʾ���ȶԻ���
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ����С���������");
			progressDialog.setCanceledOnTouchOutside(false);

		}
		progressDialog.show();

	}

	/**
	 * �رս��ȶԻ���
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	/**
	 * ����back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б���ʡ�б�������ֱ���˳���
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			finish();
		}

	}

}