package app.maqsadah.count_and_play.core

// Phase transitions and the shared helpers `Lesson` is built from. Kept beside
// it rather than inside it so neither file grows past reading size.

internal fun LessonState.counting(zone: Zone, target: Int, then: After) =
    copy(step = Step.Counting(zone, target, 1, then))

internal fun LessonState.opening(dropping: List<Token>, instruction: Line) = Outcome(
    state = this,
    script = script {
        show(StageChange.Drop(dropping.map { it.id }))
        cue(Sfx.THUD)
        // Let the set exist for a moment before it is named.
        pause(Pace.BREATH)
        say(instruction)
    },
)

/** A tap that changes nothing still gets a sound. Silence reads as broken. */
internal fun LessonState.inert() = Outcome(this, script { cue(Sfx.HOLLOW) })

private fun Script.then(more: Script) = Script(beats + more.beats)

/** Runs once a count has reached its target, carrying the cardinal beats with it. */
internal fun advance(state: LessonState, step: Step.Counting, lead: Script): Outcome {
    val task = state.task
    return when (step.then) {
        After.FINISH -> Outcome(
            state = state.copy(step = Step.Finished),
            script = lead,
            result = TaskResult(
                skill = task.skill,
                // For a straight count the answer is never "wrong" — the signal
                // that matters is whether one-to-one held, i.e. no re-taps.
                correct = state.retaps == 0,
                given = step.target,
                expected = task.answer,
                retaps = state.retaps,
            ),
        )

        // The coda after a missed Give-N: he counts what he actually put in.
        // No correction, no re-request, and no second result recorded.
        After.CODA -> Outcome(state.copy(step = Step.Finished), lead)

        After.COUNT_B -> {
            val join = task as Task.Join
            Outcome(
                state = state.counting(Zone.DISH_B, join.b, After.PREDICT_JOIN),
                script = lead.then(
                    script {
                        pause(Pace.BREATH)
                        show(StageChange.Drop(state.tokens.inZone(Zone.DISH_B).map { it.id }))
                        cue(Sfx.THUD)
                        pause(Pace.BEAT)
                        say(Line.CountThem)
                    },
                ),
            )
        }

        After.HIDE -> {
            val hidden = task as Task.UnderTheLeaf
            val arriving = state.tokens.inZone(Zone.DISH_B).map { it.id }
            val leaving = state.tokens.inZone(Zone.BOWL)
                .sortedByDescending { it.slot }
                .take(maxOf(0, -hidden.delta))
                .map { it.id }
            Outcome(
                state = state.copy(
                    step = Step.Predicting,
                    tokens = state.tokens
                        .moveTo(arriving.toSet(), Zone.HIDDEN)
                        .moveTo(leaving.toSet(), Zone.DISH_B),
                ),
                script = lead.then(
                    script {
                        pause(Pace.BEAT)
                        show(StageChange.Cover(Zone.BOWL))
                        cue(Sfx.RUSTLE)
                        pause(Pace.BEAT)
                        if (arriving.isNotEmpty()) {
                            show(StageChange.Travel(arriving, Zone.HIDDEN))
                        } else {
                            show(StageChange.Travel(leaving, Zone.DISH_B))
                        }
                        cue(Sfx.THUD)
                        pause(Pace.BREATH)
                        say(Line.WhatsUnder)
                        say(Line.MakeItHere)
                        show(StageChange.ShowPrediction)
                    },
                ),
            )
        }

        // The most important five seconds in the app: he commits to a number
        // before the evidence arrives, which turns the counting that follows
        // from labour into verification.
        After.PREDICT_JOIN -> {
            val join = task as Task.Join
            Outcome(
                state = state.copy(step = Step.Predicting),
                script = lead.then(
                    script {
                        pause(Pace.BREATH)
                        say(Line.PartsNamed(join.a, join.b))
                        pause(Pace.BEAT)
                        say(Line.HowManyAltogether)
                        say(Line.MakeItHere)
                        show(StageChange.ShowPrediction)
                    },
                ),
            )
        }

        After.TAKE_OUT -> {
            val sep = task as Task.Separate
            Outcome(
                state = state.copy(step = Step.Taking),
                script = lead.then(
                    script {
                        pause(Pace.BREATH)
                        say(Line.TakeOut(sep.take))
                    },
                ),
            )
        }

        After.CHECK -> check(state, step, lead)
    }
}

/** Compares the prediction with what was actually made, then pours back. */
private fun check(state: LessonState, step: Step.Counting, lead: Script): Outcome {
    val task = state.task
    val actual = step.target
    val correct = state.predicted == actual
    val back = state.tokens.filter { it.zone == Zone.BOWL || it.zone == Zone.DISH_B }

    return Outcome(
        state = state.copy(step = Step.Finished),
        script = lead.then(
            script {
                if (correct) {
                    cue(Sfx.CHIME)
                } else {
                    // No sad sound, nothing red, no "try again". The two frames
                    // simply sit side by side and the fact is stated.
                    pause(Pace.SETTLE)
                }
                when (task) {
                    is Task.Join -> settle(Line.MakesTotal(task.a, task.b, actual))
                    else -> if (actual == 0) settle(Line.NothingLeft) else settle(Line.WeMade(actual))
                }
                // Reversibility is what turns addition from an event into a
                // relation: if the join can be undone, the whole genuinely
                // contains the parts rather than replacing them.
                pause(Pace.BREATH)
                show(StageChange.Travel(back.map { it.id }, Zone.BOWL))
                cue(Sfx.RUSTLE)
                if (task is Task.Join) settle(Line.AndBackAgain(task.a, task.b))
            },
        ),
        result = TaskResult(
            skill = task.skill,
            correct = correct,
            given = state.predicted ?: 0,
            expected = actual,
            retaps = state.retaps,
        ),
    )
}

/** The child says "done" — the only moment the app reads his answer. */
internal fun onDone(state: LessonState): Outcome = when (state.step) {
    Step.Giving -> doneGiving(state)
    Step.Taking -> doneTaking(state)
    Step.Predicting -> donePredicting(state)
    else -> state.inert()
}

private fun doneGiving(state: LessonState): Outcome {
    val task = state.task as Task.GiveMe
    val given = state.tokens.countIn(Zone.BOWL)
    val correct = given == task.n
    val result = TaskResult(Skill.GIVE_N, correct, given, task.n, state.retaps)

    if (correct) {
        return Outcome(
            state = state.copy(step = Step.Finished),
            script = script {
                cue(Sfx.CHIME)
                settle(Line.GaveIt(task.n))
            },
            result = result,
        )
    }
    // He counts what he actually put in. That is the lesson, not a correction.
    return Outcome(
        state = state.counting(Zone.BOWL, given, After.CODA),
        script = script {
            pause(Pace.BEAT)
            say(Line.LetsCount)
        },
        result = result,
    )
}

private fun doneTaking(state: LessonState): Outcome {
    val task = state.task as Task.Separate
    val gone = state.tokens.countIn(Zone.DISH_B)
    val left = state.tokens.countIn(Zone.BOWL)

    check(left >= 0) { "took more than there were" }
    // What stays and what went are both new sets now, so their count marks
    // clear. Only `origin` survives a move, and that is deliberate.
    val recount = state.tokens.map {
        if (it.zone == Zone.BOWL || it.zone == Zone.DISH_B) it.copy(state = TokenState.Idle) else it
    }
    return Outcome(
        state = state.copy(
            step = Step.Predicting,
            tokens = recount.compact(Zone.BOWL),
        ),
        script = script {
            // He is never stopped at the requested amount — choosing when to
            // stop is Give-N in disguise. If he overshoots he is simply told,
            // and what is actually left is what gets counted.
            if (gone != task.take) say(Line.TooMany(gone, task.take))
            // The empty cells hold for a moment so he sees where they were,
            // then the rest compact left. The trailing gaps do the ghost's job
            // better than a faded object ever did.
            pause(Pace.SETTLE)
            show(StageChange.Compact(Zone.BOWL))
            pause(Pace.BEAT)
            say(Line.HowManyLeft)
            say(Line.MakeItHere)
            show(StageChange.ShowPrediction)
        },
    )
}

private fun donePredicting(state: LessonState): Outcome {
    val predicted = state.tokens.countIn(Zone.ANSWER)
    val staged = state.copy(predicted = predicted)

    return when (val task = state.task) {
        is Task.UnderTheLeaf -> {
            val actual = task.answer
            val correct = predicted == actual
            Outcome(
                state = staged.copy(step = Step.Finished),
                script = script {
                    pause(Pace.BEAT)
                    show(StageChange.Uncover(Zone.BOWL))
                    cue(Sfx.RUSTLE)
                    pause(Pace.BREATH)
                    if (correct) cue(Sfx.CHIME) else pause(Pace.SETTLE)
                    if (actual == 0) settle(Line.NothingLeft) else settle(Line.WeMade(actual))
                },
                result = TaskResult(Skill.HIDDEN, correct, predicted, actual, state.retaps),
            )
        }

        is Task.Join -> {
            // Every object travels a visible arc into the bowl and keeps the
            // colour of the dish it came from, so the parts stay visibly inside
            // the whole. Their count marks clear, because the whole is a new
            // set to count — but their origin never does.
            val pouring = state.tokens
                .filter { it.zone == Zone.DISH_A || it.zone == Zone.DISH_B }
                .map(Token::id)
                .toSet()
            Outcome(
                state = staged.copy(
                    tokens = state.tokens
                        .map { if (it.id in pouring) it.copy(state = TokenState.Idle) else it }
                        .moveTo(pouring, Zone.BOWL),
                    step = Step.Counting(Zone.BOWL, task.answer, 1, After.CHECK),
                ),
                script = script {
                    pause(Pace.BEAT)
                    show(StageChange.Travel(pouring.toList(), Zone.BOWL))
                    cue(Sfx.RUSTLE)
                    pause(Pace.BREATH)
                    say(Line.AllTogetherNow)
                    say(Line.CountThem)
                },
            )
        }

        is Task.Separate -> {
            val left = state.tokens.countIn(Zone.BOWL)
            // Zero gets its own small moment rather than being short-circuited
            // away: the empty frame, the label settling to nought, named plainly.
            if (left == 0) {
                Outcome(
                    state = staged.copy(step = Step.Finished),
                    script = script {
                        pause(Pace.BEAT)
                        settle(Line.NothingLeft)
                    },
                    result = TaskResult(Skill.SEPARATE, predicted == 0, predicted, 0, state.retaps),
                )
            } else {
                Outcome(
                    state = staged.copy(
                        step = Step.Counting(Zone.BOWL, left, 1, After.CHECK),
                    ),
                    script = script {
                        pause(Pace.BEAT)
                        say(Line.CountThem)
                    },
                )
            }
        }

        else -> staged.inert()
    }
}
