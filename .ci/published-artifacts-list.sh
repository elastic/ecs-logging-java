#!/bin/env bash
find . -name '*.jar'|grep '/target/'|grep -v javadoc|grep -v sources|grep -v '\-tests.jar'
