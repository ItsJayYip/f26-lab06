# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** No.

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

The consumer passes null as the last argument to createBooking. With the new overload, null could match either parameter type, so Java cannot choose which method to call and compilation fails.


### What happened

**The result.** What the build printed for each module.

The api module passed all 5 tests, and the consumer module passed all 7 tests. All three reactor modules reported SUCCESS, ending with BUILD SUCCESS

**If your prediction was wrong,** say what you missed.

I expected an ambiguity, but missed that the new overload has an additional fifth parameter. Existing four-argument calls still select the original method.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

No. For example, adding two same-length overloads with different reference-type parameters can make an existing call using null ambiguous.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No. The consumer still calls the old positional createBooking methods, so the consumer module will fail during compilation after those methods are replaced.

**Where.** Name the call sites you expect to be affected, if any.

The calls in FrontDesk.bookWalkIn() and FrontDesk.joinWaitlist()

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

After rewriting them to use BookingRequest, they should pass. That only proves the new API works; it does not prove the untouched consumer is compatible.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

Parent: SUCCESS. API: SUCCESS; 5 tests ran with 0 failures or errors.
Consumer: FAILURE during compilation. `FrontDesk.java:[27,19]` and
`FrontDesk.java:[33,19]` call `createBooking` with four arguments, but the
method now requires one `BookingRequest`.

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

The API tests ran and passed; the consumer tests did not run because its main
code did not compile. The producer's API tests cannot detect this caller
contract break; building the consumer can.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

Added deprecated four- and five-argument overloads:
`createBooking(String, long, long, String)` and
`createBooking(String, long, long, String, String)`. Both construct a
`BookingRequest` and delegate to `createBooking(BookingRequest)`; the
four-argument overload supplies null notes.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

`[WARNING] .../FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated`

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The unchanged consumer now builds and all 7 of its tests pass. The API owner
can adopt the request object now, while consumer owners can migrate their old
calls later.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

The compiler puts warnings at `FrontDesk.java:[27,19]` and `[33,19]` directly
in the consumer's build output, so its developers see the affected call sites
without having to find or read the API's README.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The meaning of the boolean in `cancelBooking(long, boolean)` is unclear, so a
caller can easily reverse `true` and `false`.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

`consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:48` calls
`api.cancelBooking(bookingId, true)`, which does not reveal what `true` means.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

The code still compiles, but the wrong behavior occurs silently: a quiet
cancellation may promote a waitlisted guest, or an intended notification may
leave every guest waiting.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

Replace the flag with `CancellationMode { NOTIFY_WAITLIST, QUIET }`. The new
signature is `cancelBooking(long bookingId, CancellationMode mode)`, and the
call becomes `api.cancelBooking(bookingId, CancellationMode.NOTIFY_WAITLIST)`.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The enum names the behavior at the call site, and the compiler prevents passing
an unrelated boolean value.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

It adds a type, makes calls longer, and requires existing callers to migrate.

**When the price is worth paying.** A condition under which it is.

It is worth it when choosing the wrong mode has user-visible consequences, such
as notifying or failing to notify a waitlisted guest.
