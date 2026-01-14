import React, { Component, ErrorInfo, ReactNode } from 'react';
import ErrorDialog from '@src/js/components/common/error/ErrorDialog.jsx';
import { formatError, FormattedError } from '@src/js/components/database/new-forms/utils/errorUtil.ts';

interface Props {
  children: ReactNode;
  /** Optional fallback UI component */
  fallback?: (error: FormattedError, resetError: () => void) => ReactNode;
  /** Called when error is caught (for logging) */
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  error: FormattedError | null;
  hasError: boolean;
}

/**
 * Error Boundary specifically for form components
 * 
 * Catches rendering errors and errors in lifecycle methods.
 * Does NOT catch errors in:
 * - Event handlers (use try/catch)
 * - Async operations (use try/catch)
 * - setTimeout/setInterval callbacks (use try/catch)
 * 
 * Usage:
 * ```tsx
 * <FormErrorBoundary>
 *   <EntityFormContextProvider {...props} />
 * </FormErrorBoundary>
 * ```
 */
export class FormErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      error: null,
      hasError: false,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    // Format error for consistent display
    const formattedError = formatError(error, 'A rendering error occurred');
    
    return {
      error: formattedError,
      hasError: true,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Log error for debugging
    console.error('[FormErrorBoundary] Caught error:', {
      error,
      errorInfo,
      componentStack: errorInfo.componentStack,
      formatted: this.state.error,
    });

    // Call optional error handler
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  handleReset = () => {
    this.setState({
      error: null,
      hasError: false,
    });
  };

  render() {
    if (this.state.hasError && this.state.error) {
      // Use custom fallback if provided
      if (this.props.fallback) {
        return this.props.fallback(this.state.error, this.handleReset);
      }

      // Default: Show error dialog
      const ErrorDialogAny = ErrorDialog as any;
      return (
        <ErrorDialogAny
          open={true}
          error={this.state.error.message}
          onClose={this.handleReset}
        />
      );
    }

    return this.props.children;
  }
}

export default FormErrorBoundary;

