package com.hazelcast.projection.impl;

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import com.hazelcast.projection.Projection;
import com.hazelcast.projection.Projections;
import com.hazelcast.query.QueryException;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class SingleAttributeProjectionTest extends HazelcastTestSupport {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void singleAttribute() {
        IMap<String, Person> map = populateMapWithPersons(getMapWithNodeCount(1));

        Collection<Double> result = map.project(Projections.<String, Person, Double>singleAttribute("age"));

        assertThat(result, containsInAnyOrder(1.0d, 4.0d, 7.0d));
    }

    @Test
    public void singleAttribute_key() {
        IMap<String, Person> map = populateMapWithPersons(getMapWithNodeCount(1));

        Collection<String> result = map.project(Projections.<String, Person, String>singleAttribute("__key"));

        assertThat(result, containsInAnyOrder("key1", "key2", "key3"));
    }

    @Test
    public void singleAttribute_this() {
        IMap<String, Integer> map = getMapWithNodeCount(1);
        map.put("key1", 1);
        map.put("key2", 2);

        Collection<Integer> result = map.project(Projections.<String, Integer, Integer>singleAttribute("this"));

        assertThat(result, containsInAnyOrder(1, 2));
    }

    @Test
    public void singleAttribute_emptyMap() {
        IMap<String, Person> map = getMapWithNodeCount(1);

        Collection<Double> result = map.project(Projections.<String, Person, Double>singleAttribute("age"));

        assertEquals(0, result.size());
    }

    @Test
    public void singleAttribute_null() {
        IMap<String, Person> map = getMapWithNodeCount(1);
        map.put("key1", new Person(1.0d));
        map.put("007", new Person(null));

        Collection<Double> result = map.project(Projections.<String, Person, Double>singleAttribute("age"));

        assertThat(result, containsInAnyOrder(null, 1.0d));
    }

    @Test
    public void singleAttribute_nonExistingProperty() {
        IMap<String, Person> map = populateMapWithPersons(getMapWithNodeCount(1));

        Projection<Map.Entry<String, Person>, Double> projection = Projections.singleAttribute("age123");

        expected.expect(QueryException.class);
        map.project(projection);
    }

    private IMap<String, Person> populateMapWithPersons(IMap map) {
        map.put("key1", new Person(1.0d));
        map.put("key2", new Person(4.0d));
        map.put("key3", new Person(7.0d));
        return map;
    }

    public <K, V> IMap<K, V> getMapWithNodeCount(int nodeCount) {
        if (nodeCount < 1) {
            throw new IllegalArgumentException("node count < 1");
        }

        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(nodeCount);


        Config config = new Config();
        config.setProperty("hazelcast.partition.count", "3");
        MapConfig mapConfig = new MapConfig();
        mapConfig.setName("aggr");
        mapConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
        config.addMapConfig(mapConfig);

        HazelcastInstance instance = factory.newInstances(config)[0];
        return instance.getMap("aggr");
    }

    public static class Person implements DataSerializable {
        public Double age;

        public Person() {
        }

        public Person(Double age) {
            this.age = age;
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeObject(age);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            age = in.readObject();
        }
    }

}
