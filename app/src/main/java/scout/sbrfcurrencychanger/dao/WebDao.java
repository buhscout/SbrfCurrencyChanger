package scout.sbrfcurrencychanger.dao;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import scout.sbrfcurrencychanger.R;
import scout.sbrfcurrencychanger.Repository;
import scout.sbrfcurrencychanger.entities.Account;
import scout.sbrfcurrencychanger.entities.Currency;
import scout.sbrfcurrencychanger.entities.CurrencyRate;
import scout.sbrfcurrencychanger.entities.NameValuePair;

import static scout.sbrfcurrencychanger.enums.AccountTypes.Card;

/**
 * Web data provider
 */
public class WebDao {

	private static final String mHost = "https://online.sberbank.ru";
	private static final String mCharset = "windows-1251";
    private Context mContext;

    /**
     * Constructor
     * @param context Context
     */
    public WebDao(Context context) {
        mContext = context;
    }

    /**
     * Thread for Web query
     */
    private class QueryTask extends AsyncTask<NameValuePair, Void, WebReply> {
        @Override
        protected WebReply doInBackground(NameValuePair... args) {
            String url = null;
            String requestMethod = null;
            ArrayList<NameValuePair> params = new ArrayList<>();
            for (NameValuePair arg : args) {
                String name = arg.getName();
                if (name.equalsIgnoreCase("url")) {
                    url = arg.getValue();
                } else if (name.equalsIgnoreCase("method")) {
                    requestMethod = arg.getValue();
                } else {
                    params.add(arg);
                }
            }
            if (requestMethod == null) {
                requestMethod = "GET";
            }
            HttpURLConnection connection = null;
            try {
                if (url == null) {
                    throw new Exception("Query address not found");
                }
                connection = (HttpURLConnection) new URL(url).openConnection();
                if (connection == null) {
                    return null;
                }
                connection.setInstanceFollowRedirects(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("User-Agent", "Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.16");
                connection.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                connection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/webp, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
                connection.setRequestMethod(requestMethod);
                if (params.size() > 0) {
                    OutputStream outputStream = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, mCharset));
                    writer.write(encodeUrlParameters(params));
                    writer.flush();
                    writer.close();
                    outputStream.close();
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return null;
                }
                InputStreamReader reader = new InputStreamReader(connection.getInputStream(), mCharset);
                StringBuilder data = new StringBuilder();
                char[] buffer = new char[4096];
                int len;
                while ((len = reader.read(buffer)) >= 0) {
                    data.append(buffer, 0, len);
                }
                return new WebReply(url, data.toString(), connection.getURL().toString());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        /**
         * Encoding query parameters
         *
         * @param params Query parameters
         * @throws UnsupportedEncodingException
         */
        private String encodeUrlParameters(List<NameValuePair> params) throws UnsupportedEncodingException {
            String result = "";
            for (NameValuePair pair : params) {
                if (!result.equals("")) {
                    result += "&";
                }
                result += URLEncoder.encode(pair.getName(), mCharset) + "=" + URLEncoder.encode(pair.getValue(), mCharset);
            }
            return result;
        }
    }

    /**
     * Reply web request
     */
    private class WebReply {
        /**
         * Request
         */
        private String mRequest;

        /**
         * Current location
         */
        private String mLocation;

        /**
         * Request reply
         */
        private String mReply;

        /**
         * Constructor
         * @param location Current location
         * @param reply Request reply
         */
        public WebReply(String request, String reply, String location) {
            mRequest = request;
            mLocation = location;
            mReply = reply;
        }

        /**
         * Current location
         */
        public String getLocation() {
            return mLocation;
        }

        /**
         * Request reply
         */
        public String getReply() {
            return mReply;
        }

        /**
         * Request
         */
        public String getRequest() {
            return mRequest;
        }
    }


	public boolean isLoggedIn() {
		return true;
		//return mIsLoggedIn;
	}

	private boolean mIsLoggedIn;

	/**
	 * POST Query
	 *
	 * @param url           Address
	 * @param urlParameters query parameters
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	private WebReply queryRawPost(String url, NameValuePair... urlParameters) throws InterruptedException, ExecutionException, IOException {
		return queryRaw(url, "POST", urlParameters);
	}

	/**
	 * GET Query without reply
	 *
	 * @param url           Address
	 * @param urlParameters Query parameters
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	private void queryGetNonResult(String url, NameValuePair... urlParameters) throws InterruptedException, ExecutionException, IOException {
		queryNonResult(url, "GET", urlParameters);
	}

	/**
	 * POST Query
	 *
	 * @param url           Address
	 * @param urlParameters Query parameters
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	private WebReply queryPost(String url, NameValuePair... urlParameters) throws InterruptedException, ExecutionException, IOException {
        return queryRaw(url, "POST", urlParameters);
	}

	/**
	 * GET Query
	 *
	 * @param url           Address
	 * @param urlParameters Query parameters
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	private WebReply queryGet(String url, NameValuePair... urlParameters) throws InterruptedException, ExecutionException, IOException {
        return queryRaw(url, "GET", urlParameters);
    }

	/**
	 * Query
	 *
	 * @param url           Address
	 * @param requestMethod Query type
	 * @param urlParameters Query parameters
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	private WebReply queryRaw(String url, String requestMethod, NameValuePair... urlParameters) throws InterruptedException, ExecutionException, IOException {
		QueryTask task = new QueryTask();
		NameValuePair[] mUrlParameters = new NameValuePair[urlParameters.length + 2];
		mUrlParameters[0] = new NameValuePair("url", url);
		mUrlParameters[1] = new NameValuePair("method", requestMethod);
		System.arraycopy(urlParameters, 0, mUrlParameters, 2, urlParameters.length);
		return task.execute(mUrlParameters).get();
	}

    HtmlCleaner cleaner;

    private TagNode cleanHtml(String html) {
        if (html == null) {
            return null;
        }
        if(cleaner == null) {
            cleaner = new HtmlCleaner();
            CleanerProperties props = cleaner.getProperties();
            props.setAllowHtmlInsideAttributes(true);
            props.setAllowMultiWordAttributes(true);
            props.setRecognizeUnicodeChars(true);
            props.setOmitComments(true);
            props.setUseEmptyElementTags(true);
            props.setOmitCdataOutsideScriptAndStyle(true);
            props.setTrimAttributeValues(true);
            props.setCharset(mCharset);
            props.setOmitHtmlEnvelope(true);
            props.setRecognizeUnicodeChars(true);
        }
        return cleaner.clean(html);
    }

	/**
	 * Query without reply
	 *
	 * @param url           Address
	 * @param requestMethod Query type
	 * @param urlParameters Query parameters
	 */
	private void queryNonResult(String url, String requestMethod, NameValuePair... urlParameters) {
		QueryTask task = new QueryTask();
		NameValuePair[] mUrlParameters = new NameValuePair[urlParameters.length + 2];
		mUrlParameters[0] = new NameValuePair("url", url);
		mUrlParameters[1] = new NameValuePair("method", requestMethod);
		System.arraycopy(urlParameters, 0, mUrlParameters, 2, urlParameters.length);
		task.execute(mUrlParameters);
	}

	public class SbrfException extends Exception {
		public SbrfException() {
			super();
		}

		public SbrfException(String message) {
			super(message);
		}
	}

	/**
	 * Sign in
	 */
	public boolean Login() throws SbrfException {
		String login = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.preferences_login), null);
        String pass = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.preferences_password), null);
        if(login == null || pass == null) {
            return false;
        }
		try {
			WebReply response = queryPost(mHost + "/CSAFront/login.do", new NameValuePair("field(login)", login), new NameValuePair("field(password)", pass), new NameValuePair("operation", "button.begin"));
			if (response == null) {
				throw new SbrfException();
			}
            TagNode responseNode = cleanHtml(response.getReply());
			Object[] objects = responseNode.evaluateXPath("//form[@name='LoginForm']/input[@name='$$redirect']");
			if (objects.length == 0) {
				throw new SbrfException();
			}
			String url = ((TagNode) objects[0]).getAttributeByName("value");
			if (url == null) {
				throw new SbrfException();
			}
			queryGetNonResult(url);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			throw new SbrfException("Login error: " + e.getMessage());
		}
	}


	/**
	 * Currency rates history
	 *
	 * @param dateFrom Date from
	 * @param dateTo   date to
	 */
	public CurrencyRate[] getCurrenciesRates(Date dateFrom, Date dateTo) {
		WebReply response = null;
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy", Locale.getDefault());
			response = queryRawPost("http://data.sberbank.ru/common/js/get_quote_values.php",
					new NameValuePair("version", "0"),
					new NameValuePair("inf_block", "168"),
					new NameValuePair("quotes_for", ""),
					new NameValuePair("cbrf", "0"),
					new NameValuePair("group", "1"),
					new NameValuePair("period", "on"),
					new NameValuePair("qid[]", "3"),
					new NameValuePair("qid[]", "2"),
					new NameValuePair("_date_afrom114", dateFormat.format(dateFrom)),
					new NameValuePair("_date_ato114", dateFormat.format(dateTo)),
					new NameValuePair("display", "json"));
		} catch (InterruptedException | ExecutionException | IOException e) {
			e.printStackTrace();
		}
		if (response == null) {
			return null;
		}
		List<CurrencyRate> currencyRates = new ArrayList<>();
		try {
			JSONObject json = new JSONObject(response.getReply());
			JSONArray namesJson = json.names();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
			for (int i = 0; i < namesJson.length(); i++) {
				JSONObject currencyJson = json.getJSONObject((String) namesJson.get(i)).getJSONObject("meta");
				Currency currency = null;
				String currencyCode = currencyJson.getString("TTL");
				for (Currency item : Repository.getCurrencies()) {
					if (item.getCode().equals(currencyCode)) {
						currency = item;
						break;
					}
				}
				if (currency == null) {
					currency = new Currency(currencyCode);
				}
				JSONObject ratesJson = json.getJSONObject((String) namesJson.get(i)).getJSONObject("quotes");
				JSONArray datesJson = ratesJson.names();
				for (int j = 0; j < datesJson.length(); j++) {
					JSONObject rateJson = ratesJson.getJSONObject(datesJson.getString(j));
					try {
						currencyRates.add(new CurrencyRate(currency, dateFormat.parse(datesJson.getString(j)), Float.valueOf(rateJson.getString("buy")), Float.valueOf(rateJson.getString("sell"))));
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return currencyRates.toArray(new CurrencyRate[currencyRates.size()]);
	}

	/**
	 * Current currencies rates
	 */
	public ArrayList<CurrencyRate> getCurrentCurrenciesRates() throws SbrfException {
		if (!mIsLoggedIn) {
			mIsLoggedIn = Login();
		}
		if (!mIsLoggedIn) {
			throw new SbrfException("Login error");
		}
		TagNode responseNode = null;
		try {
            WebReply response = queryGet(mHost + "/PhizIC/private/accounts.do");
            if (response == null) {
                throw new SbrfException();
            }
            responseNode = cleanHtml(response.getReply());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (responseNode == null) {
			return null;
		}
		ArrayList<CurrencyRate> currencyRates = new ArrayList<>();
		Currency[] currencies = Repository.getCurrencies();
		try {
			Object[] rates = responseNode.evaluateXPath("//div[@id='pageContent']//div[@id='right-section']//div[@class = 'currencyRate']//div[@class = 'rateValues']");
			for (Object rate : rates) {
				String currencyCode;
				String buyValue;
				String sellValue;

				Object[] nodes = ((TagNode) rate).evaluateXPath(".//div[@class='currencyRateName']");
				if (nodes.length == 0) {
					continue;
				}
				TagNode node = (TagNode) nodes[0];
				currencyCode = node.getText().toString();

				Currency currency = null;
				for (Currency cur : currencies) {
					if (Objects.equals(cur.getCode(), currencyCode)) {
						currency = cur;
						break;
					}
				}
				if (currency == null) {
					continue;
				}
				nodes = ((TagNode) rate).evaluateXPath(".//div[@class='rateText']");
				if (nodes.length == 0) {
					continue;
				}
				node = (TagNode) nodes[0];
				buyValue = node.getText().toString().trim();

				node = (TagNode) nodes[1];
				sellValue = node.getText().toString().trim();

				if (currencyCode.isEmpty() || buyValue.isEmpty() || sellValue.isEmpty()) {
					continue;
				}
				currencyRates.add(new CurrencyRate(currency, Calendar.getInstance().getTime(), Float.parseFloat(buyValue), Float.parseFloat(sellValue)));
			}
		} catch (XPatherException e) {
			e.printStackTrace();
		}
		return currencyRates;
	}

	/**
	 * Accounts list
	 *вап
	 * @throws SbrfException
	 */
	public Account[] getAccounts() throws SbrfException {
		if (!mIsLoggedIn) {
			mIsLoggedIn = Login();
		}
		if (!mIsLoggedIn) {
			throw new SbrfException("Login error");
		}
		TagNode responseNode;
		try {
            WebReply response = queryGet(mHost + "/PhizIC/private/accounts.do");
            if (response == null) {
                throw new SbrfException();
            }
            responseNode = cleanHtml(response.getReply());
		} catch (Exception e) {
			e.printStackTrace();
			throw new SbrfException("Load data error");
		}
		if (responseNode == null) {
			throw new SbrfException("Load data error");
		}
		Account[] accounts = null;
		try {
			Object[] products = responseNode.evaluateXPath("//div[@class='pageContent']//div[@class='workspace-box']//div[@class = 'productTitle']");
			accounts = new Account[products.length];

			for (int i = 0; i < products.length; i++) {
				TagNode product = (TagNode) products[i];
				String name;
				String accountNumber;
				String href;
				Currency currency = null;

				Object[] nodes = product.evaluateXPath(".//div[@class='productName']//span[@class='relative titleBlock']");
				if (nodes.length == 0) {
					continue;
				}
				TagNode node = (TagNode) nodes[0];
				name = node.getAttributeByName("Title");

				nodes = node.evaluateXPath("./a");
				if (nodes.length == 0) {
					continue;
				}
                nodes = node.evaluateXPath(".//span/@onclick");
				if (nodes.length == 0) {
					continue;
				}
                href = (String)nodes[0];
                href = href.substring(href.indexOf('/'), href.lastIndexOf('\''));

				nodes = product.evaluateXPath("//div[@class='productNumberBlock']/div");
				if (nodes.length == 0) {
					continue;
				}
				node = (TagNode) nodes[0];
				accountNumber = node.getText().toString();

				nodes = product.evaluateXPath("//div[@class='productAmount']//span");
				if (nodes.length == 0) {
					continue;
				}
				node = (TagNode) nodes[0];
				String balance = node.getText().toString();
				int index = balance.lastIndexOf(" ");
				String curSymbol = balance.substring(index + 1);
				balance = balance.substring(0, index);
				for (Currency cur : Repository.getCurrencies()) {
					if (cur.getSymbol().equalsIgnoreCase(curSymbol)) {
						currency = cur;
						break;
					}
				}
				Account account = new Account(currency, accountNumber, name, href);
				account.setBalance(Float.parseFloat(balance.replace(',', '.').replace(" ", "")));
				accounts[i] = account;
			}
		} catch (XPatherException e) {
			e.printStackTrace();
		}
		return accounts != null ? accounts : new Account[0];
	}

    public Boolean changeCurrency(Account srcAccount, Account destAccount, float sellSumm, float rate) throws SbrfException {
        if (!mIsLoggedIn) {
            mIsLoggedIn = Login();
        }
        if (!mIsLoggedIn) {
            throw new SbrfException("Login error");
        }
        SbrfException changeException = new SbrfException(String.format("Change error: %1$s => %2$s", srcAccount.getCurrency().getCode(), destAccount.getCurrency().getCode()));
        TagNode responseNode;
        String request = mHost + "/PhizIC/private/payments/payment.do";
        try {
            WebReply response = queryGet(request + "?form=ConvertCurrencyPayment");
            if (response == null) {
                throw new SbrfException();
            }
            responseNode = cleanHtml(response.getReply());
        } catch (Exception e) {
            e.printStackTrace();
            throw changeException;
        }
        if (responseNode == null) {
            throw changeException;
        }
        Object[] nodes;
        String apacheToken;
        String documentNumber;
        String documentDate;
        String pageToken;
        String srcAccountPostValue;
        String destAccountPostValue;
        try {
            nodes = responseNode.evaluateXPath("//input[@name='org.apache.struts.taglib.html.TOKEN']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            apacheToken = (String) nodes[0];

            nodes = responseNode.evaluateXPath("//div[@id='paymentForm']//div[@class='paymentValue']//input[@name='documentNumber']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            documentNumber = (String) nodes[0];

            nodes = responseNode.evaluateXPath("//div[@id='paymentForm']//div[@class='paymentValue']//input[@name='documentDate']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            documentDate = (String) nodes[0];

            nodes = responseNode.evaluateXPath("//input[@name='PAGE_TOKEN']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            pageToken = (String) nodes[0];

            if (srcAccount.getAccountType() == Card) {
                srcAccountPostValue = "card:";
            } else {
                srcAccountPostValue = "account:";
            }
            srcAccountPostValue += srcAccount.getCode();

            if (destAccount.getAccountType() == Card) {
                destAccountPostValue = "card:";
            } else {
                destAccountPostValue = "account:";
            }
            destAccountPostValue += destAccount.getCode();
        } catch (XPatherException e) {
            e.printStackTrace();
            throw changeException;
        }
        WebReply response;
        try {
            response = queryPost(request
                    , new NameValuePair("form", "ConvertCurrencyPayment")
                    , new NameValuePair("template", "")
                    , new NameValuePair("back", "false")
                    , new NameValuePair("copying", "false")
                    , new NameValuePair("exactAmount", "destination-field-exact")
                    , new NameValuePair("operation", "button.save")
                    , new NameValuePair("premierShowMsg", "undefined")
                    , new NameValuePair("org.apache.struts.taglib.html.TOKEN", apacheToken)
                    , new NameValuePair("documentNumber", documentNumber)
                    , new NameValuePair("documentDate", documentDate)
                    , new NameValuePair("fromResource", srcAccountPostValue)
                    , new NameValuePair("toResource", destAccountPostValue)
                    , new NameValuePair("sellAmount", String.format("%.2f", sellSumm).replace(',', '.'))
                    , new NameValuePair("buyAmount", String.format("%.2f", destAccount.isMain() ? (sellSumm * rate) : (sellSumm / rate)).replace(',', '.'))
                    , new NameValuePair("course", String.format("%.2f", rate).replace(',', '.'))
                    , new NameValuePair("premierShowMsg", "undefined")
                    , new NameValuePair("PAGE_TOKEN", pageToken)
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw changeException;
        }
        if (response == null) {
            throw new SbrfException();
        }
        responseNode = cleanHtml(response.getReply());
        try {
            nodes = responseNode.evaluateXPath("//form[@name='ConfirmPaymentByFormForm']/@action");
            if (nodes.length == 0) {
                throw changeException;
            }

            nodes = responseNode.evaluateXPath("//input[@name='org.apache.struts.taglib.html.TOKEN']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            apacheToken = (String) nodes[0];

            nodes = responseNode.evaluateXPath("//input[@name='PAGE_TOKEN']/@value");
            if (nodes.length == 0) {
                throw changeException;
            }
            pageToken = (String) nodes[0];

        } catch (XPatherException e) {
            e.printStackTrace();
            throw changeException;
        }
        try {
            response = queryPost(response.getLocation()
                    , new NameValuePair("org.apache.struts.taglib.html.TOKEN", apacheToken)
                    , new NameValuePair("operation", "button.dispatch")
                    , new NameValuePair("documentNumber", documentNumber)
                    , new NameValuePair("documentDate", documentDate)
                    , new NameValuePair("PAGE_TOKEN", pageToken)
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw changeException;
        }
        if (response == null) {
            throw new SbrfException();
        }
        responseNode = cleanHtml(response.getReply());
        try {
            nodes = responseNode.evaluateXPath("//div[@id='state']/span");
            if (nodes.length == 0) {
                throw changeException;
            }
            if(!Objects.equals(((TagNode) nodes[0]).getText().toString().toUpperCase(), "ИСПОЛНЕН")) {
                throw changeException;
            }
        } catch (XPatherException e) {
            e.printStackTrace();
            throw changeException;
        }
        return true;
    }
}
