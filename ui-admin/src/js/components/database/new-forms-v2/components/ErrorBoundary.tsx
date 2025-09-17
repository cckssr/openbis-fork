import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || <FormErrorBoundary error={this.state.error} />;
    }

    return this.props.children;
  }
}

// Simple fallback component
const FormErrorBoundary: React.FC<{ error?: Error }> = ({ error }) => (
  <div className="form-error-boundary">
    <h3>Something went wrong</h3>
    <p>An error occurred while rendering the form.</p>
    {error && <pre>{error.message}</pre>}
  </div>
);
