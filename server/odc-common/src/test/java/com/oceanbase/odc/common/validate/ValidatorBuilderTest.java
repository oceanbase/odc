/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.common.validate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.junit.Assert;
import org.junit.Test;

import lombok.Getter;
import lombok.Setter;

/**
 * Test class for validator builder
 *
 * @author yh263208
 * @date 2021-05-28 17:41
 * @since ODC_rleease_2.4.2
 */
public class ValidatorBuilderTest {
    @Test
    public void testPersonObjectWithoutName() {
        Person person = new Person();
        person.setTestCarMap(new HashMap<>());
        person.setTestCarList(new ArrayList<>());
        Set<ConstraintViolation<Person>> result = ValidatorBuilder.buildFastFailValidator().validate(person);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<Person>> iter = result.iterator();
        ConstraintViolation<Person> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "name");
    }

    @Test
    public void testPersonObjectWithBlankName() {
        Person person = new Person();
        person.setName("");
        person.setTestCarMap(new HashMap<>());
        Set<ConstraintViolation<Person>> result = ValidatorBuilder.buildFastFailValidator().validate(person);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<Person>> iter = result.iterator();
        ConstraintViolation<Person> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "name");
        Assert.assertEquals(entry.getMessage(), "Person name can not be blank");
    }

    @Test
    public void testPersonObjectWithoutTestCarList() {
        Person person = new Person();
        person.setName("David");
        Set<ConstraintViolation<Person>> result =
                ValidatorBuilder.buildFastFailValidator().validate(person, TestInterface1.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<Person>> iter = result.iterator();
        ConstraintViolation<Person> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null");
    }

    @Test
    public void testPersonObjectWithoutTestCarListForSecondGroup() {
        Person person = new Person();
        person.setName("David");
        Set<ConstraintViolation<Person>> result =
                ValidatorBuilder.buildFastFailValidator().validate(person, TestInterface2.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<Person>> iter = result.iterator();
        ConstraintViolation<Person> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null with second group");
    }

    @Test
    public void testPersonObjectWithoutTestCar() {
        Person person = new Person();
        person.setName("David");
        person.setTestCarList(new ArrayList<>());
        person.setTestCarMap(new HashMap<>());

        Car car = new Car();
        car.setLengths(new int[] {1});
        car.setDoorData(new HashMap<>());
        person.setTestCar(car);
        Set<ConstraintViolation<Person>> result = ValidatorBuilder.buildFastFailValidator().validate(person);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<Person>> iter = result.iterator();
        ConstraintViolation<Person> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "testCar.brand");
        Assert.assertEquals(entry.getMessage(), "Brand can not be null");
    }

    @Test
    public void testValidationWithoutValidAnnotation() throws NoSuchMethodException {
        Person testPerson = new Person();
        App app = new App();
        Method method = App.class.getDeclaredMethod("printPerson2", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson});
        Assert.assertEquals(result.size(), 0);
    }

    @Test
    public void testValidationWithoutValidAnnotation1() throws NoSuchMethodException {
        Person testPerson = new Person();
        App app = new App();
        Method method = App.class.getDeclaredMethod("printPerson2", Person.class);
        Set<ConstraintViolation<App>> result =
                ValidatorBuilder.buildFastFailExecutableValidator().validateParameters(app, method,
                        new Object[] {testPerson});
        Assert.assertEquals(result.size(), 0);
    }

    @Test
    public void testValidationWithValidAnnotation() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setTestCarMap(new HashMap<>());
        testPerson.setTestCarList(new ArrayList<>());
        Method method = App.class.getDeclaredMethod("printPerson3", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson});
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson3.arg0.name");
    }

    @Test
    public void testValidationWithValidatedAnnotation() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("Marry");
        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson}, TestInterface1.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0.testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null");
    }

    @Test
    public void testValidationWithNullPerson() throws NoSuchMethodException {
        App app = new App();
        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {null});
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0");
        Assert.assertEquals(entry.getMessage(), "must not be null");
    }

    @Test
    public void testValidationWithValidatedAnnotation1() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("Marry");
        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson}, TestInterface2.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0.testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null with second group");
    }

    @Test
    public void testValidationWithSubValidated() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("Marry");
        Car car = new Car();
        car.setLengths(new int[] {1});
        car.setDoorData(new HashMap<>());
        testPerson.setTestCar(car);
        testPerson.setTestCarList(Arrays.asList(new Car()));
        testPerson.setTestCarMap(new HashMap<>());

        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson});
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0.testCar.brand");
        Assert.assertEquals(entry.getMessage(), "Brand can not be null");
    }

    @Test
    public void testValidationWithSubValidatedPrimitive() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("Marry");
        Car car = new Car();
        car.setBrand(CarBrand.DAS_AUTO);
        car.setLengths(new int[] {1});
        testPerson.setTestCar(car);
        testPerson.setTestCarList(Arrays.asList(new Car()));
        testPerson.setTestCarMap(new HashMap<>());

        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson});
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0.testCar.doorData");
        Assert.assertEquals(entry.getMessage(), "Door data can not be null");
    }

    @Test
    public void testValidationWithSubValidatedPrimitiveList() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("Marry");
        Car car = new Car();
        car.setBrand(CarBrand.DAS_AUTO);
        car.setDoorData(new HashMap<>());
        testPerson.setTestCar(car);
        testPerson.setTestCarList(Arrays.asList(new Car()));
        testPerson.setTestCarMap(new HashMap<>());

        Method method = App.class.getDeclaredMethod("printPerson1", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateParameters(app, method, new Object[] {testPerson});
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson1.arg0.testCar.lengths");
        Assert.assertEquals(entry.getMessage(), "Lengths can not be null");
    }

    @Test
    public void testValidationReturnValue() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson = app.printPerson4(testPerson);
        Method method = App.class.getDeclaredMethod("printPerson4", Person.class);
        Set<ConstraintViolation<App>> result =
                ValidatorBuilder.buildFastFailExecutableValidator().validateReturnValue(app, method, testPerson);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson4.<return value>");
        Assert.assertEquals(entry.getMessage(), "Return value can not be null");
    }

    @Test
    public void testReturnValueValidationWithoutValidAnnotation() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson = app.printPerson5(testPerson);
        Method method = App.class.getDeclaredMethod("printPerson5", Person.class);
        Set<ConstraintViolation<App>> result =
                ValidatorBuilder.buildFastFailExecutableValidator().validateReturnValue(app, method, testPerson);
        Assert.assertEquals(result.size(), 0);
    }

    @Test
    public void testReturnValueValidationWithValidAnnotation() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson = app.printPerson6(testPerson);
        testPerson.setTestCarMap(new HashMap<>());
        testPerson.setTestCarList(new ArrayList<>());

        Method method = App.class.getDeclaredMethod("printPerson6", Person.class);
        Set<ConstraintViolation<App>> result =
                ValidatorBuilder.buildFastFailExecutableValidator().validateReturnValue(app, method, testPerson);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson6.<return value>.name");
    }

    @Test
    public void testReturnValueValidationWithValidatedAnnotation() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("David");
        testPerson = app.printPerson6(testPerson);

        Method method = App.class.getDeclaredMethod("printPerson6", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateReturnValue(app, method, testPerson, TestInterface1.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson6.<return value>.testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null");
    }

    @Test
    public void testReturnValueValidationWithValidatedAnnotation1() throws NoSuchMethodException {
        App app = new App();
        Person testPerson = new Person();
        testPerson.setName("David");
        testPerson = app.printPerson6(testPerson);

        Method method = App.class.getDeclaredMethod("printPerson6", Person.class);
        Set<ConstraintViolation<App>> result = ValidatorBuilder.buildFastFailExecutableValidator()
                .validateReturnValue(app, method, testPerson, TestInterface2.class);
        Assert.assertEquals(result.size(), 1);
        Iterator<ConstraintViolation<App>> iter = result.iterator();
        ConstraintViolation<App> entry = iter.next();
        Assert.assertEquals(entry.getPropertyPath().toString(), "printPerson6.<return value>.testCarList");
        Assert.assertEquals(entry.getMessage(), "Test car list can not be null with second group");
    }
}


/**
 * Test enum for validation
 *
 * @author yh263208
 * @date 2021-05-20 21:55
 * @since ODC_release_2.4.1
 */
enum CarBrand {
    DAS_AUTO,
    TOYOTA,
    MAZDA
}


/**
 * Test class for validation
 *
 * @author yh263208
 * @date 2021-05-20 21:55
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
class Car {
    private Car subCar;
    @NotNull(message = "Brand can not be null")
    private CarBrand brand;
    @NotNull(message = "Displacement can not be null")
    private int displacement;
    @NotNull(message = "Lengths can not be null")
    private int[] lengths;
    private Integer length;
    private String carName;
    @NotNull(message = "Door data can not be null")
    private Map<String, Integer> doorData;
}


/**
 * Test class for validation
 *
 * @author yh263208
 * @date 2021-05-20 21:55
 * @since ODC_release_2.4.1
 */
@Getter
@Setter
class Person {
    @NotBlank(message = "Person name can not be blank")
    @NotNull(message = "Person name can not be null")
    private String name;
    private Integer age;
    @NotNull(groups = {TestInterface1.class}, message = "Test car list can not be null")
    @NotNull(groups = {TestInterface2.class}, message = "Test car list can not be null with second group")
    private List<Car> testCarList;
    @Valid
    private Car testCar;
    private List<String> address;
    @Valid
    private List<Car> testCarList1;
    @NotNull
    @Valid
    private Map<String, Car> testCarMap;
}


/**
 * Test class for validation
 *
 * @author yh263208
 * @date 2021-05-20 21:55
 * @since ODC_release_2.4.1
 */
class App {

    public Person printPerson1(@Valid @NotNull Person person) {
        return person;
    }

    public Person printPerson2(Person person) {
        return person;
    }

    public Person printPerson3(@Valid Person person) {
        return person;
    }

    @NotNull(message = "Return value can not be null")
    public Person printPerson4(@NotNull(message = "Input value can not be null") Person person) {
        return null;
    }

    @NotNull(message = "Return value can not be null")
    public Person printPerson5(@NotNull(message = "Input value can not be null") Person person) {
        return person;
    }

    @NotNull(message = "Return value can not be null")
    @Valid
    public Person printPerson6(@NotNull(message = "Input value can not be null") Person person) {
        return person;
    }
}


interface TestInterface1 {
}


interface TestInterface2 {
}
