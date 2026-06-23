import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { render, RenderOptions } from '@testing-library/react';
import { ReactElement } from 'react';
import createSagaMiddleware from 'redux-saga';
import candidatesReducer from '../store/slices/candidatesSlice';
import jobsReducer from '../store/slices/jobsSlice';
import matchesReducer from '../store/slices/matchesSlice';
import uploadReducer from '../store/slices/uploadSlice';
import rootSaga from '../store/sagas';

interface ExtendedRenderOptions extends Omit<RenderOptions, 'queries'> {
  preloadedState?: any;
  store?: any;
}

const rootReducer = combineReducers({
  candidates: candidatesReducer,
  jobs: jobsReducer,
  matches: matchesReducer,
  upload: uploadReducer,
});

function createTestStore(preloadedState: any = {}) {
  const sagaMiddleware = createSagaMiddleware();
  
  const testStore = configureStore({
    reducer: rootReducer,
    preloadedState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({ thunk: false }).concat(sagaMiddleware),
  });
  
  sagaMiddleware.run(rootSaga);
  return testStore;
}

export function renderWithProviders(
  ui:ReactElement,
  {
    preloadedState,
    store,
    ...renderOptions
  }: ExtendedRenderOptions = {}
) {
  if (!store) {
    store = createTestStore(preloadedState);
  }
  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <Provider store={store}>
        <BrowserRouter>
          {children}
        </BrowserRouter>
      </Provider>
    );
  }

  return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) };
}

// Re-export everything from React Testing Library
export * from '@testing-library/react';
export { renderWithProviders as render };
