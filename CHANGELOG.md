# Changelog

## v3.1.0 - January 27, 2016

* **Breaking**: Fixed miss-conception issue with the entity manager holder injected to early in the data manager and finder manager
* **Breaking**: Removed slf4j

## v3.0.1, v3.0.2 - January 26-27, 2016

* Minor changes to identify tricky bug

## v3.0.0 - January 26, 2016

* **Breaking**: Extended the possibility to use more than one entity manager to the finders
* **Breaking**: Moved annotations in the same package for clarity
* **Breaking**: Moved class `EntityManagerHolder` in utils package

## v2.0.0 - January 25, 2016

* **Breaking**: Added the possibility to use more than one entity manager with the data generators. Now, each entity manager is injected in the data generator and all the objects in this data generator.

## v1.0.0 - October 27, 2015

* Initial fork
* Added the Support for naming scheme MyService[Interface] -> MyServiceImpl[Class] for EJB injections
