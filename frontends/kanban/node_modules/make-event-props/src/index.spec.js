import makeEventProps from '.';

describe('makeEventProps', () => {
  const fakeEvent = {};

  it('returns object with valid and only valid event callbacks', () => {
    const props = {
      onClick: jest.fn(),
      someInvalidProp: jest.fn(),
    };
    const result = makeEventProps(props);

    expect(result).toMatchObject({ onClick: expect.any(Function) });
  });

  it('calls getArgs function on event invoke if given', () => {
    const props = {
      onClick: jest.fn(),
      someInvalidProp: jest.fn(),
    };
    const getArgs = jest.fn();
    const result = makeEventProps(props, getArgs);

    // getArgs shall not be invoked before a given event is fired
    expect(getArgs).not.toHaveBeenCalled();

    result.onClick(fakeEvent);

    expect(getArgs).toHaveBeenCalledTimes(1);
    expect(getArgs).toHaveBeenCalledWith('onClick');
  });

  it('properly calls callbacks given in props given no getArgs function', () => {
    const props = {
      onClick: jest.fn(),
    };
    const result = makeEventProps(props);

    result.onClick(fakeEvent);

    expect(props.onClick).toHaveBeenCalledWith(fakeEvent);
  });

  it('properly calls callbacks given in props given getArgs function', () => {
    const props = {
      onClick: jest.fn(),
    };
    const getArgs = jest.fn();
    const args = {};
    getArgs.mockReturnValue(args);
    const result = makeEventProps(props, getArgs);

    result.onClick(fakeEvent);

    expect(props.onClick).toHaveBeenCalledWith(fakeEvent, args);
  });
});
