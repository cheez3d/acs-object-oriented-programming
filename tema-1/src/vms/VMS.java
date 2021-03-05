package vms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import util.FileScanner;
import util.Util;

import vms.campaign.Campaign;
import vms.campaign.CampaignStrategyType;

import vms.exception.VMSAccessException;
import vms.exception.VMSArgumentException;
import vms.exception.VMSException;
import vms.exception.VMSNotFoundException;
import vms.exception.VMSParseException;
import vms.exception.VMSStateException;

import vms.user.User;
import vms.user.UserType;

import vms.voucher.Voucher;

public final class VMS {

	public static final class Reader {

		private static final String DELIMITER = ";";

		private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		private static LocalDateTime readDateTime(Scanner scanner) throws VMSNotFoundException, VMSParseException {
			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No date-time found");
			}

			String dateTimeString = scanner.next();

			LocalDateTime dateTime;

			try {
				dateTime = LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
			} catch (DateTimeParseException e) {
				throw new VMSParseException("Bad date-time '%s'", e, e.getParsedString());
			}

			return dateTime;
		}

		private static Campaign readCampaignWithoutStrategy(Scanner scanner) throws VMSArgumentException, VMSNotFoundException, VMSParseException {
			int id;

			String name;
			String description;

			LocalDateTime startDateTime;
			LocalDateTime endDateTime;

			int totalVoucherCount;

			if (!scanner.hasNextInt()) {
				throw new VMSNotFoundException("No campaign id found");
			}

			id = scanner.nextInt();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No campaign name found");
			}

			name = scanner.next();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No campaign description found");
			}

			description = scanner.next();

			try {
				startDateTime = readDateTime(scanner);
			} catch (VMSNotFoundException e) {
				throw new VMSNotFoundException("No campaign start date-time found", e);
			} catch (VMSParseException e) {
				throw new VMSParseException("Bad campaign start date-time", e);
			}

			try {
				endDateTime = readDateTime(scanner);
			} catch (VMSNotFoundException e) {
				throw new VMSNotFoundException("No campaign start date-time found", e);
			} catch (VMSParseException e) {
				throw new VMSParseException("Bad campaign start date-time", e);
			}

			if (!scanner.hasNextInt()) {
				throw new VMSNotFoundException("No campaign budget found");
			}

			totalVoucherCount = scanner.nextInt();

			return new Campaign(id, name, description, startDateTime, endDateTime, totalVoucherCount);
		}

		private static Campaign readCampaign(Scanner scanner) throws VMSArgumentException, VMSNotFoundException, VMSParseException {
			Campaign campaign;

			CampaignStrategyType strategy;

			campaign = readCampaignWithoutStrategy(scanner);

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No campaign strategy found");
			}

			String strategyString = scanner.next();

			try {
				strategy = CampaignStrategyType.valueOf(strategyString);
			} catch (IllegalArgumentException e) {
				throw new VMSParseException("Bad campaign strategy '%s'", e, strategyString);
			}

			campaign.setStrategy(strategy);

			return campaign;
		}

		private static User readUser(Scanner scanner) throws VMSArgumentException, VMSNotFoundException, VMSParseException {
			int id;

			String name;
			String email;
			String password;

			UserType type;

			if (!scanner.hasNextInt()) {
				throw new VMSNotFoundException("No user id found");
			}

			id = scanner.nextInt();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No user name found");
			}

			name = scanner.next();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No user password found");
			}

			password = scanner.next();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No user email found");
			}

			email = scanner.next();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No user type found");
			}

			String typeString = scanner.next();

			try {
				type = UserType.valueOf(typeString);
			} catch (IllegalArgumentException e) {
				throw new VMSParseException("Bad user type '%s'", e, typeString);
			}

			return new User(id, name, email, password, type);
		}

		private static Test.Event readEvent(Scanner scanner) throws VMSArgumentException, VMSNotFoundException, VMSParseException {
			if (!scanner.hasNextInt()) {
				throw new VMSNotFoundException("No user id found");
			}

			int userId = scanner.nextInt();

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No event type found");
			}

			String eventType = scanner.next();

			Test.Event.Pair funcPair = Test.EVENT_MAP.get(eventType);

			if (funcPair == null) {
				throw new VMSParseException("Bad event type '%s'", eventType);
			}

			Test.Event.Reader reader = funcPair.getKey();
			Test.Event.Dispatcher dispatcher = funcPair.getValue();

			List<Object> argList = reader.apply(scanner);

			argList.add(0, userId);
			argList.add(1, eventType);

			return new Test.Event(dispatcher, Collections.unmodifiableList(argList));
		}

		private static List<Object> readVoucherArgs(Scanner scanner) throws VMSNotFoundException {
			List<Object> argList = new LinkedList<>();

			if (!scanner.hasNextInt()) {
				throw new VMSNotFoundException("No voucher campaign id found");
			}

			argList.add(scanner.nextInt());

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No voucher email found");
			}

			argList.add(scanner.next());

			if (!scanner.hasNext()) {
				throw new VMSNotFoundException("No voucher type found");
			}

			argList.add(scanner.next());

			if (!scanner.hasNextFloat()) {
				throw new VMSNotFoundException("No voucher value found");
			}

			argList.add(scanner.nextFloat());

			return argList;
		}

		private static enum ListType {

			CAMPAIGN("campaigns"),
			USER("users"),
			TEST_EVENT("events"),
			VOUCHER_ARGS("voucher arguments");

			private String string;

			private ListType(String string) {
				this.string = string;
			}

		}

		private static List<?> readList(ListType type, FileScanner fileScanner) throws VMSException {
			if (fileScanner == null || fileScanner.isClosed()) {
				throw new VMSStateException(String.format("Nothing to read %s from", type.string));
			}

			List<Object> list = new LinkedList<>();

			Scanner scanner = fileScanner.getScanner();

			try {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();

					try (Scanner lineScanner = new Scanner(line)) {
						lineScanner.useDelimiter(DELIMITER);

						switch (type) {
						case CAMPAIGN:
							list.add(readCampaign(lineScanner));
							break;

						case USER:
							list.add(readUser(lineScanner));
							break;

						case TEST_EVENT:
							list.add(readEvent(lineScanner));
							break;

						case VOUCHER_ARGS:
							list.add(readVoucherArgs(lineScanner));
						}
					} catch (VMSArgumentException | VMSNotFoundException | VMSParseException e) {
						throw new VMSException("Exception on line '%s'", e, line);
					}
				}

				scanner.close();
			} catch (VMSException e) {
				throw new VMSException("Exception for file '%s'", e, fileScanner.getFile());
			}

			return list;
		}

		private FileScanner campaignsFileScanner;
		private FileScanner usersFileScanner;
		private FileScanner eventsFileScanner;
		private FileScanner emailsFileScanner;

		private Reader() {}

		public void setCampaignsFile(File campaignsFile) throws VMSException {
			try {
				try {
					campaignsFileScanner = new FileScanner(campaignsFile);
				} catch (FileNotFoundException e) {
					throw new VMSNotFoundException("No such file", e);
				}

				Scanner scanner = campaignsFileScanner.getScanner();
				scanner.useDelimiter("\\n");

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No campaign count found");
				}

				scanner.nextLine();

				LocalDateTime dateTime = readDateTime(scanner);

				if (getInstance().dateTime == null) {
					getInstance().dateTime = dateTime;
				}

				scanner.nextLine();
			} catch (VMSNotFoundException | VMSParseException e) {
				throw new VMSException("Exception for file '%s'", e, campaignsFile);
			}
		}

		public void setUsersFile(File usersFile) throws VMSException {
			try {
				try {
					usersFileScanner = new FileScanner(usersFile);
				} catch (FileNotFoundException e) {
					throw new VMSNotFoundException("No such file", e);
				}

				Scanner scanner = usersFileScanner.getScanner();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No user count found");
				}

				scanner.nextLine();
			} catch (VMSNotFoundException e) {
				throw new VMSException("Exception for file '%s'", e, usersFile);
			}
		}

		public void setEventsFile(File eventsFile) throws VMSException {
			try {
				try {
					eventsFileScanner = new FileScanner(eventsFile);
				} catch (FileNotFoundException e) {
					throw new VMSNotFoundException("No such file", e);
				}

				Scanner scanner = eventsFileScanner.getScanner();
				scanner.useDelimiter("\\n");

				readDateTime(scanner);

				scanner.nextLine();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No event count found");
				}

				scanner.nextLine();
			} catch (VMSNotFoundException | VMSParseException e) {
				throw new VMSException("Exception for file '%s'", e, eventsFile);
			}
		}

		public void setEmailsFile(File emailsFile) throws VMSException {
			try {
				try {
					emailsFileScanner = new FileScanner(emailsFile);
				} catch (FileNotFoundException e) {
					throw new VMSNotFoundException("No such file", e);
				}

				Scanner scanner = emailsFileScanner.getScanner();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No email count found");
				}

				scanner.nextLine();
			} catch (VMSNotFoundException | VMSParseException e) {
				throw new VMSException("Exception for file '%s'", e, emailsFile);
			}
		}

		@SuppressWarnings("unchecked")
		public List<Campaign> readCampaigns() throws VMSException {
			return (List<Campaign>)readList(ListType.CAMPAIGN, campaignsFileScanner);
		}

		@SuppressWarnings("unchecked")
		public List<User> readUsers() throws VMSException {
			return (List<User>)readList(ListType.USER, usersFileScanner);
		}

		@SuppressWarnings("unchecked")
		public Queue<Test.Event> readEvents() throws VMSException {
			return (Queue<Test.Event>)readList(ListType.TEST_EVENT, eventsFileScanner);
		}

		@SuppressWarnings("unchecked")
		public List<List<Object>> readEmails() throws VMSException {
			return (List<List<Object>>)readList(ListType.VOUCHER_ARGS, emailsFileScanner);
		}

	}

	public static final class AccessController {

		public static final class Event {

			public static enum Type {

				ADD_CAMPAIGN("addCampaign"),
				EDIT_CAMPAIGN("editCampaign"),
				CANCEL_CAMPAIGN("cancelCampaign"),
				GENERATE_VOUCHER("generateVoucher"),
				REDEEM_VOUCHER("redeemVoucher"),
				GET_VOUCHERS("getVouchers"),
				GET_OBSERVERS("getObservers"),
				GET_NOTIFICATIONS("getNotifications"),
				GET_VOUCHER("getVoucher");

				public static Type fromString(String string) throws IllegalArgumentException {
					return Arrays.stream(values())
						.filter(t -> t.string.equals(string))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException(String.format("No enum with string '%s'", string)));
				}

				private String string;

				private Type(String string) {
					this.string = string;
				}

			}

			private Type type;
			private User user;
			private Iterator<? extends Object> argIterator;

			public Event(Type type, User user, Iterator<? extends Object> argIterator) {
				this.type = type;
				this.user = user;
				this.argIterator = argIterator;
			}

			public Type getType() {
				return type;
			}

			public User getUser() {
				return user;
			}

			public Iterator<? extends Object> getArgIterator() {
				return argIterator;
			}

		}

		@FunctionalInterface
		private static interface Action {

			Object apply(User user, Iterator<? extends Object> argIterator) throws VMSAccessException, VMSArgumentException, VMSStateException;

		}

		private static final Map<Event.Type, Action> ACTION_MAP;

		static {
			Map<Event.Type, Action> actionMap = new HashMap<>();

			VMSAccessException accessException = new VMSAccessException("Permission denied");

			// addCampagin
			Action addCampaign = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				Campaign campaign = (Campaign)argIterator.next();

				getInstance().addCampaign(campaign);

				return null;
			};

			// editCampaign
			Action editCampaign = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				Campaign ref = (Campaign)argIterator.next();

				getInstance().updateCampaign(ref.getId(), ref);

				return null;
			};

			// cancelCampaign
			Action cancelCampaign = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				int id = (int)argIterator.next();

//				@SuppressWarnings("unused")
//				LocalDateTime dateTime = (LocalDateTime)argIterator.next();

				getInstance().cancelCampaign(id);

				return null;
			};

			// generateVoucher
			Action generateVoucher = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				int campaignId = (int)argIterator.next();

				String email = (String)argIterator.next();

				String type = (String)argIterator.next();

				float value = (float)argIterator.next();

				Campaign campaign = getInstance().getCampaign(campaignId);

				campaign.generateVoucher(email, type, value);

				return null;
			};

			// redeemVoucher
			Action redeemVoucher = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				int campaignId = (int)argIterator.next();

				int id = (int)argIterator.next();

				LocalDateTime dateTime = (LocalDateTime)argIterator.next();

				Campaign campaign = getInstance().getCampaign(campaignId);

				campaign.redeemVoucher(id, dateTime);

				return null;
			};

			// getVouchers
			Action getVouchers = (user, argIterator) -> {
				if (!(user.getType() == UserType.GUEST)) {
					throw accessException;
				}

				return user.getVouchers();
			};

			// getObservers
			Action getObservers = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				int campaignId = (int)argIterator.next();

				Campaign campaign = getInstance().getCampaign(campaignId);

				return campaign.getObservers();
			};

			// getNotifications
			Action getNotifications = (user, argIterator) -> {
				if (!(user.getType() == UserType.GUEST)) {
					throw accessException;
				}

				return user.getNotifications();
			};

			// getVoucher
			Action getVoucher = (user, argIterator) -> {
				if (!(user.getType() == UserType.ADMIN)) {
					throw accessException;
				}

				int campaignId = (int)argIterator.next();

				Campaign campaign = getInstance().getCampaign(campaignId);

				return getInstance().execute(campaign);
			};

			actionMap.put(Event.Type.ADD_CAMPAIGN, addCampaign);
			actionMap.put(Event.Type.EDIT_CAMPAIGN, editCampaign);
			actionMap.put(Event.Type.CANCEL_CAMPAIGN, cancelCampaign);
			actionMap.put(Event.Type.GENERATE_VOUCHER, generateVoucher);
			actionMap.put(Event.Type.REDEEM_VOUCHER, redeemVoucher);
			actionMap.put(Event.Type.GET_VOUCHERS, getVouchers);
			actionMap.put(Event.Type.GET_OBSERVERS, getObservers);
			actionMap.put(Event.Type.GET_NOTIFICATIONS, getNotifications);
			actionMap.put(Event.Type.GET_VOUCHER, getVoucher);

			ACTION_MAP = Collections.unmodifiableMap(actionMap);
		}

		private AccessController() {}

		public Object handle(Event event) throws VMSException {
			Action action = ACTION_MAP.get(event.getType());

			Object result = null;

			try {
				result = action.apply(event.getUser(), event.getArgIterator());
			} catch (VMSAccessException | VMSArgumentException | VMSStateException e) {
				throw new VMSException("%s could not perform '%s'", e, event.getUser(), event.getType().string);
			}

			return result;
		}

	}

	public static final class Test {

		@SuppressWarnings("serial")
		public static final class Event extends AbstractMap.SimpleImmutableEntry<Test.Event.Dispatcher, List<Object>> {

			@FunctionalInterface
			public static interface Reader {

				List<Object> apply(Scanner scanner) throws VMSArgumentException, VMSNotFoundException, VMSParseException;

			}

			@FunctionalInterface
			public static interface Dispatcher {

				Object apply(List<Object> argList) throws VMSException;

			}

			public static final class Pair extends AbstractMap.SimpleImmutableEntry<Reader, Dispatcher> {

				public Pair(Reader reader, Dispatcher dispatcher) {
					super(reader, dispatcher);
				}

			}

			public Event(Dispatcher dispatcher, List<Object> argList) {
				super(dispatcher, argList);
			}

		}

		private static final Map<String, Event.Pair> EVENT_MAP;

		static {
			Map<String, Event.Pair> eventMap = new HashMap<>();

			// addCampagin
			Event.Reader addCampaignReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				argList.add(Reader.readCampaign(scanner));

				return argList;
			};

			// editCampaign
			Event.Reader editCampaignReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				argList.add(Reader.readCampaignWithoutStrategy(scanner));

				return argList;
			};

			// cancelCampaign
			Event.Reader cancelCampaignReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No campaign id found");
				}

				argList.add(scanner.nextInt());

				/* try {
					argList.add(Reader.readDateTime(scanner));
				} catch (VMSNotFoundException e) {
					throw new VMSNotFoundException("No campaign cancellation date-time found", e);
				} catch (VMSParseException e) {
					throw new VMSParseException("Bad campaign cancellation date-time", e);
				} */

				return argList;
			};

			// generateVoucher
			Event.Reader generateVoucherReader = (Scanner scanner) -> {
				return Reader.readVoucherArgs(scanner);
			};

			// redeemVoucher
			Event.Reader redeemVoucherReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No voucher campaign id found");
				}

				argList.add(scanner.nextInt());

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No voucher id found");
				}

				argList.add(scanner.nextInt());

				try {
					argList.add(Reader.readDateTime(scanner));
				} catch (VMSNotFoundException e) {
					throw new VMSNotFoundException("No voucher redemption date-time found", e);
				} catch (VMSParseException e) {
					throw new VMSParseException("Bad voucher redemption date-time", e);
				}

				return argList;
			};

			// getVouchers
			Event.Reader getVouchersReader = (Scanner scanner) -> {
				return new LinkedList<>();
			};

			// getObservers
			Event.Reader getObserversReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No campaign id found");
				}

				argList.add(scanner.nextInt());

				return argList;
			};

			// getNotifications
			Event.Reader getNotificationsReader = (Scanner scanner) -> {
				return new LinkedList<>();
			};

			// getVoucher
			Event.Reader getVoucherReader = (Scanner scanner) -> {
				List<Object> argList = new LinkedList<>();

				if (!scanner.hasNextInt()) {
					throw new VMSNotFoundException("No campaign id found");
				}

				argList.add(scanner.nextInt());

				return argList;
			};

			Event.Dispatcher sharedDispatcher = (List<Object> argList) -> {
				Iterator<Object> argIterator = argList.iterator();

				int userId = (int)argIterator.next();

				User user = getInstance().getUser(userId);

				String eventTypeString = (String)argIterator.next();

				AccessController.Event.Type eventType = AccessController.Event.Type.fromString(eventTypeString);

				AccessController.Event event = new AccessController.Event(eventType, user, argIterator);

				Object result = getInstance().accessController.handle(event);

				return result;
			};

			eventMap.put("addCampaign", new Event.Pair(addCampaignReader, sharedDispatcher));
			eventMap.put("editCampaign", new Event.Pair(editCampaignReader, sharedDispatcher));
			eventMap.put("cancelCampaign", new Event.Pair(cancelCampaignReader, sharedDispatcher));
			eventMap.put("generateVoucher", new Event.Pair(generateVoucherReader, sharedDispatcher));
			eventMap.put("redeemVoucher", new Event.Pair(redeemVoucherReader, sharedDispatcher));
			eventMap.put("getVouchers", new Event.Pair(getVouchersReader, sharedDispatcher));
			eventMap.put("getObservers", new Event.Pair(getObserversReader, sharedDispatcher));
			eventMap.put("getNotifications", new Event.Pair(getNotificationsReader, sharedDispatcher));
			eventMap.put("getVoucher", new Event.Pair(getVoucherReader, sharedDispatcher));

			EVENT_MAP = Collections.unmodifiableMap(eventMap);
		}

		private List<Campaign> campaignList;
		private List<User> userList;
		private Queue<Event> eventQueue;

		public Test(File campaignsFile, File usersFile, File eventsFile) throws VMSException {
			Reader reader = getInstance().getReader();

			reader.setCampaignsFile(campaignsFile);
			reader.setUsersFile(usersFile);
			reader.setEventsFile(eventsFile);

			this.campaignList = reader.readCampaigns();
			this.userList = reader.readUsers();
			this.eventQueue = reader.readEvents();
		}

		public Test(List<Campaign> campaignList, List<User> userList, Queue<Event> eventQueue) {
			this.campaignList = campaignList;
			this.userList = userList;
			this.eventQueue = eventQueue;
		}

		public void run(boolean restorePreviousState) {
			List<Campaign> oldCampaignList = getInstance().getCampaigns();
			List<User> oldUserList = getInstance().getUsers();

			getInstance().clear();

			getInstance().addCampaigns(campaignList);
			getInstance().addUsers(userList);

			for (Event event : eventQueue) {
				try {
					Event.Dispatcher dispatcher = event.getKey();
					List<Object> argList = event.getValue();

					Object result = dispatcher.apply(argList);

					if (result != null) {
						System.out.println(result);
					}
				} catch (VMSException e) {
					Util.printExceptionCauseChain(e);
				}
			}

			if (restorePreviousState) {
				getInstance().clear();

				getInstance().addCampaigns(oldCampaignList);
				getInstance().addUsers(oldUserList);
			}
		}

		public void run() {
			run(false);
		}

	}

	public static final class Config {

		private static final String CONFIG_FILE_PATH = "config.xml";

		private Properties config;

		private Config() throws VMSParseException {
			config = new Properties();

			try {
				InputStream stream = new FileInputStream(CONFIG_FILE_PATH);

				config.loadFromXML(stream);

				stream.close();
			} catch (InvalidPropertiesFormatException e) {
				throw new VMSParseException("Could not parse '%s'", e, CONFIG_FILE_PATH);
			} catch (IOException e) {}
		}

		public String get(String key) {
			return config.getProperty(key);
		}

		public void set(String key, String value) {
			config.setProperty(key, value);
		}

		public void remove(String key) {
			config.remove(key);
		}

		public void save() throws VMSException {
			try {
				OutputStream stream = new FileOutputStream(CONFIG_FILE_PATH);

				config.storeToXML(stream, "VMS Configuration");

				stream.close();
			} catch (IOException e) {
				throw new VMSException("Could not save '%s'", e, CONFIG_FILE_PATH);
			}
		}

	}

	private static VMS instance;

	public static VMS getInstance() {
		if (instance == null) {
			instance = new VMS();
		}

		return instance;
	}

	private LocalDateTime dateTime;

	private Reader reader;

	private AccessController accessController;

	private Config config;

	private Map<Integer, Campaign> campaignMap;
	private Map<Integer, User> userMap;

	private User authenticatedUser;

	private VMS() {
		reader = new Reader();

		accessController = new AccessController();

		config = new Config();

		campaignMap = new TreeMap<>();
		userMap = new TreeMap<>();
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;

		getCampaigns().forEach(Campaign::updateStatus);
	}

	public Reader getReader() {
		return reader;
	}

	public AccessController getAccessController() {
		return accessController;
	}

	public Config getConfig() {
		return config;
	}

	public Campaign getCampaign(int id) throws VMSArgumentException {
		if (!campaignMap.containsKey(id)) {
			throw new VMSArgumentException("No campaign with id %d found", id);
		}

		return campaignMap.get(id);
	}

	public int getCampaignCount() {
		return campaignMap.size();
	}

	public List<Campaign> getCampaigns() {
		List<Campaign> campaignList = campaignMap.values().stream().collect(Collectors.toList());

		return Collections.unmodifiableList(campaignList);
	}

	public void addCampaign(Campaign campaign) {
		campaignMap.put(campaign.getId(), campaign);
	}

	public void addCampaigns(Collection<Campaign> collection) {
		Map<Integer, Campaign> map = collection.stream()
			.collect(Collectors.toMap(Campaign::getId, Function.identity()));

		campaignMap.putAll(map);
	}

	public void addCampaigns(File file) throws VMSException {
		reader.setCampaignsFile(file);

		List<Campaign> campaignList = reader.readCampaigns();

		addCampaigns(campaignList);
	}

	public void updateCampaign(int id, Campaign ref) throws VMSArgumentException, VMSStateException {
		Campaign campaign = getCampaign(id);

		campaign.update(ref);
	}

	public void cancelCampaign(int id) throws VMSStateException {
		Campaign campaign = getCampaign(id);

		campaign.cancel();
	}

	public User getUser(int id) throws VMSArgumentException {
		if (!userMap.containsKey(id)) {
			throw new VMSArgumentException("No user with id %d found", id);
		}

		return userMap.get(id);
	}

	public User getUserFromEmail(String email) throws VMSArgumentException {
		return userMap.values().stream()
			.filter(u -> u.getEmail().compareTo(email) == 0)
			.findFirst()
			.orElseThrow(() -> new VMSArgumentException("No user with email '%s' found", email));
	}

	public int getUserCount() {
		return userMap.size();
	}

	public List<User> getUsers() {
		List<User> userList = userMap.values().stream().collect(Collectors.toList());

		return userList;
	}

	public void addUser(User user) {
		userMap.put(user.getId(), user);
	}

	public void addUsers(Collection<User> collection) {
		Map<Integer, User> map = collection.stream()
			.collect(Collectors.toMap(User::getId, Function.identity()));

		userMap.putAll(map);
	}

	public void addUsers(File file) throws VMSException {
		reader.setUsersFile(file);

		List<User> userList = reader.readUsers();

		addUsers(userList);
	}

	public User getAuthenticatedUser() {
		return authenticatedUser;
	}

	public void setAuthenticatedUser(User authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}

	public Voucher execute(Campaign campaign) throws VMSStateException {
		return campaign.executeStrategy();
	}

	public void clearCampaigns() {
		campaignMap.clear();
	}

	public void clearUsers() {
		userMap.clear();
	}

	public void clear() {
		clearCampaigns();
		clearUsers();
	}

	public void stop() {
		clear();

		config.save();
	}

}