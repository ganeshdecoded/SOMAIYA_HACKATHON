package com.example.a7minutesworkout

import com.example.medicinereminder.R

object Constants {

    fun defaultExerciseList(): ArrayList<ExerciseModel>{
        val exerciseList = ArrayList<ExerciseModel>()


        val jumpingJacks = ExerciseModel(1, "Cat Bend", R.drawable.ex1)
        exerciseList.add(jumpingJacks)

        val wallSit = ExerciseModel(2, "Standing forward bend chair", R.drawable.ex2)
        exerciseList.add(wallSit)

        val pushUp = ExerciseModel(3, "Legs Up", R.drawable.ex3)
        exerciseList.add(pushUp)

        val abdominalCrunch = ExerciseModel(4, "Seated forward Bend", R.drawable.ex4)
        exerciseList.add(abdominalCrunch)

        val stepUpOnChair = ExerciseModel(5, "Raise the head", R.drawable.ex5)
        exerciseList.add(stepUpOnChair)

        val squat = ExerciseModel(6, "Bridge Pose", R.drawable.ex6)
        exerciseList.add(squat)

        val tricepsDipOnChair = ExerciseModel(7, "Knee Streching", R.drawable.ex7)
        exerciseList.add(tricepsDipOnChair)

        val plank = ExerciseModel(8, "Side lying leg rise", R.drawable.ex8)
        exerciseList.add(plank)

        val highKneesRunningInPlace = ExerciseModel(9, "Hand forward squat", R.drawable.ex9)
        exerciseList.add(highKneesRunningInPlace)

        val lunges = ExerciseModel(10, "Lunges", R.drawable.ic_lunge)
        exerciseList.add(lunges)

        val pushUpAndRotation = ExerciseModel(11, "Push up and Rotation", R.drawable.ic_push_up_and_rotation)
        exerciseList.add(pushUpAndRotation)

        val sidePlank = ExerciseModel(12, "Side Plank", R.drawable.ic_side_plank)
        exerciseList.add(sidePlank)

        return exerciseList
    }

}