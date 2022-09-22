{
  // We need to check to make sure we don't already have EventTarget
  // in our testing environment. If we do, the tests don't matter.
  // so it needs to fail in that case.
  let eventTargetAlreadyPresent = true;
  try {
    new EventTarget();
  } catch (err) {
    // Expected this to error, this is good
    eventTargetAlreadyPresent = false;
  }

  if (eventTargetAlreadyPresent) {
    fail(
      "EventTarget existed before tests, tests are irrelevant in this environment."
    );
  }
}

// Now we pull it in.
require("./index.js");

if (typeof EventTarget !== "function") {
  fail("EventTarget does not exist");
}

if (typeof Event !== "function") {
  fail("Event does not exist");
}

{
  // Event should allow the proper options, but nothing weird
  const event = new Event('test', { cancelable: true, bubbles: true, composed: true, garbage: 'lol' });

  if (!'cancelable' in event || !event.cancelable) {
    fail('Event.cancelable not properly set')
  }

  if (!'bubbles' in event || !event.bubbles) {
    fail('Event.bubbles not properly set')
  }

  if (!'composed' in event || !event.composed) {
    fail('Event.composed not properly set')
  }

  if ('garbage' in event) {
    fail('Should not add arbitrary things to the event');
  }
}

{
  // Should pass the proper stuff to the listener function
  const et = new EventTarget();

  const event = new Event("test1");

  et.addEventListener("test1", function (e) {
    if (e.type !== "test1") {
      fail(`Incorrect event type ${e.type}`);
    }
    if (e !== event) {
      fail("Event instance not passed to listener");
    }
    if (this !== et) {
      fail(`context should be the EventTarget that dispatched`);
    }
  });

  et.dispatchEvent(event);
}

{
  // adding and removing event listeners should work

  let handlerCalls = 0;
  const handler = () => handlerCalls++;
  const listener = {
    handleEvent() {
      this.calls++;
    },
    calls: 0,
  };

  const et = new EventTarget();

  et.addEventListener("registration", handler);
  et.addEventListener("registration", listener);

  et.dispatchEvent(new Event("registration"));
  et.dispatchEvent(new Event("registration"));

  if (listener.calls !== 2) {
    fail(`Expected 2 calls to listener.handleEvent, got ${listener.calls}.`);
  }
  if (handlerCalls !== 2) {
    fail(`Expected 2 calls to handler, got ${handlerCalls}.`);
  }

  et.removeEventListener("registration", handler);

  et.dispatchEvent(new Event("registration"));
  et.dispatchEvent(new Event("registration"));

  if (listener.calls !== 4) {
    fail(`Expected 4 calls to listener.handleEvent, got ${listener.calls}.`);
  }
  if (handlerCalls !== 2) {
    fail(`Expected 2 calls to handler, got ${handlerCalls}.`);
  }

  et.removeEventListener("registration", listener);
  et.dispatchEvent(new Event("registration"));
  et.dispatchEvent(new Event("registration"));

  if (listener.calls !== 4) {
    fail(`Expected 4 calls to listener.handleEvent, got ${listener.calls}.`);
  }
  if (handlerCalls !== 2) {
    fail(`Expected 2 calls to handler, got ${handlerCalls}.`);
  }
}

{
  // Registering the same handler more than once should be idempotent
  const et = new EventTarget();

  let handlerCalls = 0;
  const handler = () => {
    handlerCalls++;
  };

  et.addEventListener("idem", handler);
  et.addEventListener("idem", handler, { once: true });
  et.addEventListener("idem", handler);
  et.addEventListener("idem", handler);

  et.dispatchEvent(new Event("idem"));

  if (handlerCalls !== 1) {
    fail(
      `Expected handler to have been called once. Was called ${handlerCalls} times`
    );
  }
}

{
  // Should handle registration of listeners that only fire once
  // using the options argument
  const et = new EventTarget();

  let calls = 0;
  et.addEventListener(
    "testOnce",
    function () {
      calls++;
    },
    { once: true }
  );

  et.dispatchEvent(new Event("testOnce"));
  et.dispatchEvent(new Event("testOnce"));
  et.dispatchEvent(new Event("testOnce"));

  if (calls !== 1) {
    fail(`Registering once did not work. Expected 1 call, got ${calls}.`);
  }
}

{
  // addEventListener Should not throw if boolean is passed as the third argument
  const et = new EventTarget();

  et.addEventListener("test", function () {}, true);
}

{
  // Should handle registration of listener objects.
  const et = new EventTarget();

  const listener = {
    handleEvent() {
      if (this !== listener) {
        fail("Expected context to be the listener object itself");
      }
      this.calls++;
    },
    calls: 0,
  };

  et.addEventListener("listenerObject", listener);

  et.dispatchEvent(new Event("listenerObject"));
  et.dispatchEvent(new Event("listenerObject"));
  et.dispatchEvent(new Event("listenerObject"));

  if (listener.calls !== 3) {
    fail(
      `handleEvent should have been called 3 times, called ${listener.calls} times.`
    );
  }
}

{
  // dispatchEvent should return true
  const et = new EventTarget();
  const defaultNotPrevented = et.dispatchEvent(new Event("test"));

  if (defaultNotPrevented !== true) {
    fail(
      "basic EventTarget does not have cancellable events, so dispatchEvent should always return true"
    );
  }
}

{
  // Events should be dispatched synchronous
  const et = new EventTarget();

  const order = [];
  let n = 0;
  et.addEventListener("sync", () => {
    order.push(n++, "event");
  });

  order.push(n++, "start");
  et.dispatchEvent(new Event("sync"));
  et.dispatchEvent(new Event("sync"));
  et.dispatchEvent(new Event("sync"));
  order.push(n++, "end");

  if (
    order.join(",") !==
    [0, "start", 1, "event", 2, "event", 3, "event", 4, "end"].join(",")
  ) {
    fail("Events not triggered synchronously");
  }
}

// Kill the node process with a failure message if a test
// is failing.
function fail(reason) {
  console.error(reason);
  process.exit(1);
}

// We've reached the end of this test script
console.log("All tests pass");
