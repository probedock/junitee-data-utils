# junitee-data-utils

> A way to manage the database of a Java EE project from API tests run outside the application container.

When it's time to write API tests, it's quite frequent to manipulate the data directly in the database. You want to avoid using your own API to populate and manipulate the data in your application. It's not rare that your API does not allow to modify your data in the database without some application workflow.

Sometimes, you need to forge the data of your application to run a specific API test. Therefore, you need the proper tools to manipulate database from your tests. This framework is an attempt to fill the gap for this.

This framework bring to you the management of the transaction when populating the data. Each data generator has a populate method and a clean method. The idea behind this is to have the following mechanism:

1. @BeforeClass
2. @Before
3. Data population
4. @Test
5. Clean data
6. @After
7. @AfterClass

The data generator has also an addition feature that allow you create methods starting by `create`, `update` and `delete` that will be, by convention, managed in transaction even if they are used inside a test. This will allow to interact with the persistence layer directly from the test methods.

As a test developer, you have the duty to provide the correct behavior to populate the data and at the end of your test to provide the proper way to cleanup the data.

In addition, you can inject any EJB inside your data generators and finders through the `@EJB` annotation. This framework will take care to manage the sub-injections for you and also take care of cyclic references. Only one EJB of each will be created and injected one or multiple times.

## Usage

1. Put the following dependency in your pom.xml

```xml
<dependency>
  <groupId>io.probedock.test</groupId>
  <artifactId>junitee-data-utils</artifactId>
  <version>3.0.0</version>
  <scope>test</scope>
</dependency>
```

2. Once the dependency is in place and downloaded, you can start to setup your tests. First, create an `abstract` test class.

  ```java
  public class AbstractTest {
    // Define a rule chain to enable all the rules from junitee-data-utils
    @Rule
  	public RuleChain chain;

    // Define the data generator rule that will be part of the chain rule and
    // give access to the data generators inside the test methods
  	protected DataGeneratorManager dataGeneratorManager;

  	// Define the data finder rule that will be part of the chain rule and
    // give access to the persistence layer for querying purposes
  	protected FinderManager finderManager;

    public AbstractTest() {
      // Create the persistence manager factory that refers to a persistence.xml
      // and the persistence unit. This file must be present in test/resources/META-INF
      EntityManagerFactory defaultEntityManagerFactory = Persistence.createEntityManagerFactory("test");
      
      // Create another entity manager factory (optional). In fact, some cases requires to manipulate more
      // than one DB at a time. Therefore, it is possible to annotate the data generators and/or the finders
      // with @EntityManagerName to set the correct entity manager for the corresponding context.
      EntityManagerFactory secondEntityManagerFactory = Persistence.createEntityManagerFactory("second");

      // Create the entity manager holder with the default entity manager factory
      EntityManagerHolder emh = new EntityManagerHolder(defaultEntityManagerFactory);
      
      // Add the second entity manager factory to the holder
      emh.addFactory("SECOND", secondEntityManagerFactory);

      // Once your holder is ready, call the build method on it. If not, you will get
      // exceptions when creating the datamanager and/or finder
      emh.build();

      // Instantiate the data manager rule with the holder
      dataGeneratorManager = new DataGeneratorManager(emh);

      // Same for the finder manager
  	  finderManager = new FinderManager(emh);

      // Finally, setup the chain rule to make sure the data generator rule is applied
      // before the finder manager rule.
      // Don't forget to assign the chain updated to the instance variable.
      chain = RuleChain.emptyRuleChain().around(dataGeneratorManager).around(finderManager);
    }
  }
  ```

  This abstract class is the base for all your tests where you need to access the database and where you do not have a application container at your disposal.

  You can find more info about the [@rule](https://github.com/junit-team/junit/wiki/Rules) mechanism on the JUnit website.

3. You can create a data generator. Let's take an example of a `User` model.

  ```java
  public class User {
    @Id
    private long id;

    private String firstname;
    private String lastname;

    public long getId() {
      return id;
    }

    // No setter for the id
    // additional getters/setters
  }
  ```

  Then, we can implement the data generator like that.

  ```java
  // If you want to use another entity manager, use this annotation and specify the name 
  // you have chosen when you have setup the abstract test class
  // @EntityManagerName("SECOND")
  public class UserDataGenerator implements IDataGenerator {
    // The persistence context. It will be injected by junitee-data-utils
    // The default entity manager will be used if the annotation @EntityManagerName 
    // is not present. Otherwise, the corresponding entity manager is used.
  	@PersistenceContext
  	private EntityManager em;

    // Reference to the user entity for the cleanup phase and also to make it
    // available to the test methods.
  	private User user;

  	@Override
  	public void generate() {
      // Create and persist a new user, keep the reference for later use.
  		user = new User("first", "last");
  		em.persist(user);
  	}

  	@Override
  	public void cleanup() {
      // Remove the user to let the persistence layer clean.
  		em.remove(user);
  	}

    // Make possible to retrieve the user from the test methods.
  	public User getUser() {
  		return user;
  	}
  }
  ```

4. Time to use the data generator in a test. There is the test class example.

  ```java
  // Inherit from AbstractApiTest to benefit the generator stuff and co
  public class UserApiTest extends AbstractApiTest {
    @Test
    // Setup one or more generators. They will be run in the order of declaration
    @DataGenerator(UserDataGenerator.class)
  	public void itShouldMakeTheLifeEasier() {
      // Retrieve the reference to the generator. At this stage, the data are already
      // generated and available for the test method.
      UserDataGenerator udg = dataGeneratorManager.getDataGenerator(UserDataGenerator.class);

      // Retrieve the generated user
  		User generatedUser = udg.getUser();

  		// Make anything you want with this generated user.
      ...
  	}
  }
  ```

5. More fun with the generators. Let's introduce the code by convention for the generators. Based on our previous example for the `UserDataGenerator` class, we will add a new method to create new user usable from the test methods.

  ```java
  public class UserDataGenerator {
    ... // The stuff we already discuss before

    // To store a reference of any user created during test execution
    private List<User> users = new ArrayList<>();

    @Override
  	public void cleanup() {
      // Remove the user to let the persistence layer clean.
  		em.remove(user);

      // Now, let's remove the user added during the test execution. It's a
      // naive implementation to remove user after user in place of a bulk delete.
      for (User u : users) {
        em.remove(u);
      }
  	}

    // The magic happens here. The name of the method is starting by create and
    // then junitee-data-utils will take care to manage this method through
    // a proper transaction management.
    public User createUser(String firstname, String lastname) {
      // We create, persit, store a reference and return the created user
      User user = new User(firstname, lastname);
      em.persist(user);
      users.add(user);
      return user;
    }
  }
  ```

6. And now in the tests.

    ```java
    // Inherit from AbstractApiTest to benefit the generator stuff and co
    public class UserApiTest extends AbstractApiTest {
      @Test
      // Setup one or more generators. They will be run in the order of declaration
      @DataGenerator(UserDataGenerator.class)
    	public void itShouldMakeTheLifeEasier() {
        // Retrieve the reference to the generator. At this stage, the data are already
        // generated and available for the test method.
        UserDataGenerator udg = dataGeneratorManager.getDataGenerator(UserDataGenerator.class);

        // Retrieve the generated user
    		User generatedUser = udg.getUser();

    		// Make anything you want with this generated user.
        ...

        // This call is managed inside a transaction. Take care that once the
        // user is returned, he is no more attached to the persistence context.
        User newlyGeneratedUser = udg.createUser("newFirst", "newLast");
    	}
    }
    ```

    You can do the same with the following prefixes:

    * `create` to create new models;
    * `update` to update existing models;
    * `delete` to delete existing models.

7. It's time to introduce the finders. Sometimes, you only want a way to make some query to the persistence layer. In this case, in place of creating a data generator with empty `generate` and `cleanup` methods, you can create a finder.

  ```java
  // The IFinder interface is used as a marker to let the framework manipulate the
  // finder and then to manage the injections.
  // You also have the possibility to change the entity manager used for the finder
  // @EntityManagerName("SECOND")
  public class UserFinder implements IFinder {
    // The framework will inject the persistence context. The default entity manager 
    // will be used if the annotation @EntityManagerName is not present. Otherwise,
    // the corresponding entity manager is used.
    @PersistenceContext
    private EntityManager em;

    public List<User> findAll() {
      // We create a query to get all the users and return the result.
      Query query = em.createQuery("SELECT u FROM User u");
      return query.getResultList();
    }
  }
  ```

  For the finder, we also use code by convention. All the methods starting by `find` will trigger a cache clear on the persistence layer.

8. We can now use the finder inside our test methods. For that, we also have a dedicated annotation.

  ```java
  // Same class that we already discussed
  public class UserApiTest extends AbstractApiTest {
    @Test
    @DataGenerator(UserDataGenerator.class)
    @Finder(UserFinder.class)
    public void itShouldMakeTheLifeEasier() {
      ... // The stuff we implemented during this how to.

      // We retrieve the reference to the User finder.
      UserFinder userFinder = finderManager.getFinder(UserFinder.class);

      // We use the finder to retrieve the list of users.
      List<User> users = userFinder.findAll()
    }
  }
  ```

9. You have all the pieces in hands to write your test and take advantage of this persistence wrapper for API testing.

### Requirements

* Java 6+

## Contributing

* [Fork](https://help.github.com/articles/fork-a-repo)
* Create a topic branch - `git checkout -b feature`
* Push to your branch - `git push origin feature`
* Create a [pull request](http://help.github.com/pull-requests/) from your branch

Please add a changelog entry with your name for new features and bug fixes.

## License

**junitee-data-utils** is licensed under the [MIT License](http://opensource.org/licenses/MIT).
See [LICENSE.txt](LICENSE.txt) for the full text.
