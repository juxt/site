[![npm](https://img.shields.io/npm/v/make-event-props.svg)](https://www.npmjs.com/package/make-event-props) ![downloads](https://img.shields.io/npm/dt/make-event-props.svg) [![CI](https://github.com/wojtekmaj/make-event-props/workflows/CI/badge.svg)](https://github.com/wojtekmaj/make-event-props/actions) ![dependencies](https://img.shields.io/david/wojtekmaj/make-event-props.svg
) ![dev dependencies](https://img.shields.io/david/dev/wojtekmaj/make-event-props.svg
) [![tested with jest](https://img.shields.io/badge/tested_with-jest-99424f.svg)](https://github.com/facebook/jest)

# Make-Event-Props
A function that, given props, returns an object of event callback props optionally curried with additional arguments.

This package allows you to pass event callback props to a rendered DOM element without the risk of applying any invalid props that could cause unwanted side effects.

## tl;dr
* Install by executing `npm install make-event-props` or `yarn add make-event-props`.
* Import by adding `import makeEventProps from 'make-event-props'`.
* Create your event props object:
    ```js
    get eventProps() {
      return makeEventProps(this.props, (eventName) => additionalArgs);
    }
    ```
* Use your event props:
    ```js
    render() {
      return (
        <div {...this.eventProps} />
      );
    }
    ```

## License

The MIT License.

## Author

<table>
  <tr>
    <td>
      <img src="https://github.com/wojtekmaj.png?s=100" width="100">
    </td>
    <td>
      Wojciech Maj<br />
      <a href="mailto:kontakt@wojtekmaj.pl">kontakt@wojtekmaj.pl</a><br />
      <a href="https://wojtekmaj.pl">https://wojtekmaj.pl</a>
    </td>
  </tr>
</table>
