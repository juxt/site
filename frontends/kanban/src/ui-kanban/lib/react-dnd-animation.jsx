/* eslint-disable */
import { Component } from 'react';

let animationId;
const sigmoid = (x) => x / (1 + Math.abs(x));
const initialState = {
  transform: null,
  prevX: 0,
  rotation: 0,
};

class NaturalDragAnimation extends Component {
  static defaultProps = {
    animationRotationFade: 0.9,
    rotationMultiplier: 1.3,
    sigmoidFunction: sigmoid,
  };

  static getDerivedStateFromProps(props, state) {
    if (props.snapshot.dropAnimation && state.transform) {
      return {
        ...initialState,
      };
    }

    return null;
  }

  state = {
    ...initialState,
  };

  // added to support React.Portal
  componentDidMount() {
    if (this.props.snapshot.isDragging) {
      animationId = requestAnimationFrame(this.patchTransform);
    }
  }

  componentDidUpdate(prevProps) {
    if (!prevProps.snapshot.isDragging && this.props.snapshot.isDragging) {
      animationId = requestAnimationFrame(this.patchTransform);
    }

    if (prevProps.snapshot.isDragging && !this.props.snapshot.isDragging) {
      cancelAnimationFrame(animationId);
    }
  }

  componentWillUnmount() {
    cancelAnimationFrame(animationId);
  }

  patchTransform = () => {
    const {
      snapshot: { isDragging },
      style,
      animationRotationFade,
      rotationMultiplier,
      sigmoidFunction,
    } = this.props;

    if (isDragging && style.transform) {
      const currentX = style.transform
        .match(/translate\(.{1,}\)/g)[0]
        .match(/-?[0-9]{1,}/g)[0];

      const velocity = currentX - this.state.prevX;
      const prevRotation = this.state.rotation;

      let rotation =
        prevRotation * animationRotationFade +
        sigmoidFunction(velocity) * rotationMultiplier;

      const newTransform = `${style.transform} rotate(${rotation}deg)`;

      if (Math.abs(rotation) < 0.01) rotation = 0;

      this.setState(
        {
          transform: newTransform,
          prevX: currentX,
          rotation,
        },
        () => {
          animationId = requestAnimationFrame(this.patchTransform);
        },
      );
    } else {
      animationId = requestAnimationFrame(this.patchTransform);
    }
  };

  render() {
    const {
      snapshot: { isDragging, dropAnimation },
    } = this.props;

    const style =
      isDragging && !dropAnimation
        ? {
            ...this.props.style,
            transform: this.state.transform,
          }
        : this.props.style;

    return <>{this.props.children(style)}</>;
  }
}

export default NaturalDragAnimation;
