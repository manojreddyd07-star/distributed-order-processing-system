import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ValidationPage from './ValidationPage';

// Mock the ValidationTable component
jest.mock('../../components/validation/ValidationTable', () => {
  return function MockValidationTable() {
    return <div data-testid="validation-table">Validation Table</div>;
  };
});

describe('ValidationPage Component', () => {
  
  test('renders validation page with header', () => {
    render(<ValidationPage />);
    
    expect(screen.getByText('Validations')).toBeInTheDocument();
    expect(screen.getByText('View and monitor order validation results')).toBeInTheDocument();
  });

  test('renders ValidationTable component', () => {
    render(<ValidationPage />);
    
    expect(screen.getByTestId('validation-table')).toBeInTheDocument();
  });

  test('has correct page structure', () => {
    const { container } = render(<ValidationPage />);
    
    const validationPage = container.querySelector('.validation-page');
    expect(validationPage).toBeInTheDocument();
    
    const header = container.querySelector('.validation-page-header');
    expect(header).toBeInTheDocument();
  });

  test('displays main heading as h1 element', () => {
    render(<ValidationPage />);
    
    const heading = screen.getByRole('heading', { name: /Validations/i });
    expect(heading).toBeInTheDocument();
    expect(heading.tagName).toBe('H1');
  });

  test('displays descriptive text', () => {
    render(<ValidationPage />);
    
    const description = screen.getByText('View and monitor order validation results');
    expect(description).toBeInTheDocument();
  });

  test('renders without crashing', () => {
    const { container } = render(<ValidationPage />);
    expect(container).toBeInTheDocument();
  });

  test('component exports correctly', () => {
    expect(ValidationPage).toBeDefined();
  });
});
