package com.animalswithcoolhats.gradekeeper.android.api

import java.util.Date

data class StudyBlock(var id: String, var userId: String, var name: String, var startDate: Date, var endDate: Date, var subjects: List<Course>)

data class Course(var id: String, var studyBlockId: String, var longName: String, var courseCodeName: String, var courseCodeNumber: String, var color: String, var components: List<CourseComponent>)

data class CourseComponent(var id: String, var subjectId: String, var name: String, var nameOfSubcomponentSingular: String, var subjectWeighting: Double, var numberOfSubComponentsToDrop_Lowest: Int, var subcomponents: List<CourseSubcomponent>)
data class CourseSubcomponent(var id: String, var componentId: String, var numberInSequence: Int, var overrideName: String?, var isCompleted: Boolean, var gradeValuePercentage: Double)
